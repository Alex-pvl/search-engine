package ru.nstu.searchengine.crawler

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.nstu.searchengine.models.*
import ru.nstu.searchengine.utils.getBodyAsText
import ru.nstu.searchengine.utils.isPreposition
import ru.nstu.searchengine.utils.splitWithIndex
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class Crawler {
	private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
	private val scope = CoroutineScope(dispatcher + SupervisorJob())
	private val statistics = CopyOnWriteArrayList<StatisticResponse>()

	suspend fun crawl(urls: List<String>, maxDepth: Int = 2) {
		val visitedUrls = ConcurrentHashMap.newKeySet<String>()
		var currentDepthUrls = urls

		for (depth in 1..maxDepth) {
			val newUrls = ConcurrentHashMap.newKeySet<String>()
			val jobs = mutableListOf<Job>()

			for (url in currentDepthUrls) {
				if (visitedUrls.add(url)) {
					val job = scope.launch {
						processUrl(url)?.let { newUrls.addAll(it) }
					}
					jobs.add(job)
				}
			}

			jobs.joinAll()
			currentDepthUrls = newUrls.toList()
		}
		log.info("Crawling ended")
	}

	fun getHtml(url: String) = Jsoup.connect(url).get().getBodyAsText()

	fun getLinks(url: String) = processUrl(url)

	fun getStatistics() = statistics

	private fun processUrl(url: String): List<String>? {
		return try {
			val document = Jsoup.connect(url).get()
			addToIndex(document, url)
			parseLinks(document, url)
		} catch (e: Exception) {
			log.error("Error processing $url: ${e.message}", e)
			null
		} finally {
			collectStatistics(url)
		}
	}

	private fun addToIndex(document: Document, url: String) {
		runBlocking(Dispatchers.IO) {
			newSuspendedTransaction {
				val urlId = getOrCreateUrlId(url)
				val text = document.getBodyAsText()

				for ((location, word) in text.splitWithIndex()) {
					val isIgnored = word.isPreposition()
					val wordId = getOrCreateWordId(word, isIgnored) ?: continue
					WordLocations.insertIgnore {
						it[this.url] = urlId
						it[this.word] = wordId
						it[this.location] = location
					}
				}
			}
		}
	}

	private fun parseLinks(document: Document, fromUrl: String): List<String> {
		log.info("Start parsing link: $fromUrl")
		val links = mutableListOf<String>()
		runBlocking(Dispatchers.IO) {
			newSuspendedTransaction {
				val fromUrlId = getOrCreateUrlId(fromUrl)
				val elements = document.getElementsByTag("a")

				for ((visited, element) in elements.withIndex()) {
					if (visited >= MAX_LINKS_COUNT) break

					val href = element.absUrl("href")

					if (href.isNotBlank() && href.startsWith("http")) {
						log.info("[$fromUrlId]: href=$href")

						val toUrlId = getOrCreateUrlId(href)
						val linkId = getOrCreateLinkId(fromUrlId, toUrlId)
						val linkText = element.text()

						for ((_, word) in linkText.splitWithIndex()) {
							val isIgnored = word.isPreposition()
							val wordId = getOrCreateWordId(word, isIgnored) ?: continue
							LinkWords.insertIgnore {
								it[this.word] = wordId
								it[this.link] = linkId
							}
						}

						links.add(href)
					}
				}
			}
		}
		return links
	}

	private fun collectStatistics(url: String) {
		runBlocking(Dispatchers.IO) {
			newSuspendedTransaction {
				val stats = Statistics(
					urlsCount = Urls.selectAll().count(),
					wordsCount = Words.selectAll().count(),
					wordLocationsCount = WordLocations.selectAll().count(),
					linksCount = Links.selectAll().count(),
					linkWordsCount = LinkWords.selectAll().count()
				)
				val statisticResponse = StatisticResponse(url, stats)
				statistics.addIfAbsent(statisticResponse)
			}
		}
	}

	private fun getOrCreateUrlId(url: String): Int {
		return Urls.select { Urls.url eq url }.map { it[Urls.id].value }.firstOrNull()
			?: Urls.insertAndGetId {
				it[Urls.url] = url
			}.value
	}

	private fun getOrCreateWordId(word: String, isIgnored: Boolean): Int? {
		return try {
			Words.select { Words.word eq word.lowercase() }.map { it[Words.id].value }.firstOrNull()
				?: Words.insertIgnoreAndGetId {
					it[Words.word] = word.lowercase()
					it[Words.isIgnored] = isIgnored
				}?.value
		} catch (e: Exception) {
			null // ON CONFLICT DO NOTHING
		}
	}

	private fun getOrCreateLinkId(fromUrlId: Int, toUrlId: Int): Int {
		return Links.select { (Links.fromUrl eq fromUrlId) and (Links.toUrl eq toUrlId) }
			.map { it[Links.id].value }
			.firstOrNull()
			?: Links.insertAndGetId {
				it[this.fromUrl] = fromUrlId
				it[this.toUrl] = toUrlId
			}.value
	}

	private companion object {
		val log: Logger = LoggerFactory.getLogger(Crawler::class.java)
		const val MAX_LINKS_COUNT = 100
	}
}

@Serializable
data class StatisticResponse(
	val url: String,
	val statistics: Statistics,
)

@Serializable
data class Statistics(
	val urlsCount: Long,
	val wordsCount: Long,
	val wordLocationsCount: Long,
	val linksCount: Long,
	val linkWordsCount: Long,
)