package ru.nstu.searchengine.crawler

import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.nstu.searchengine.models.*
import ru.nstu.searchengine.utils.getBodyAsText
import ru.nstu.searchengine.utils.isPreposition
import ru.nstu.searchengine.utils.splitWithIndex
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class Crawler(
	private val meterRegistry: PrometheusMeterRegistry,
) {
	private val dispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
	private val scope = CoroutineScope(dispatcher + SupervisorJob())

	init {
		registerMetrics()
	}

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
		log.info("Crawling ended.")
	}

	private fun processUrl(url: String): List<String>? {
		log.info("Start parsing link: $url")
		return try {
			val document = Jsoup.connect(url).get()
			addToIndex(document, url)
			val domain = URL(url).host
			meterRegistry.counter("crawler_domain_count", "domain", domain).increment()
			parseLinks(document, url)
		} catch (e: Exception) {
			log.error("Error processing $url: ${e::class.simpleName}")
			null
		}
	}

	private fun addToIndex(document: Document, url: String) {
		runBlocking(Dispatchers.IO) {
			transaction {
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
					meterRegistry.counter("crawler_word_count", "word", word.lowercase()).increment()
				}
			}
		}
	}

	private fun parseLinks(document: Document, fromUrl: String): List<String> {
		val links = mutableListOf<String>()
		runBlocking(Dispatchers.IO) {
			transaction {
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

	private fun getOrCreateUrlId(url: String): Int {
		return Urls.select { Urls.url like url }.map { it[Urls.id].value }.firstOrNull()
			?: Urls.insertAndGetId {
				it[Urls.url] = url
			}.value
	}

	private fun getOrCreateWordId(word: String, isIgnored: Boolean): Int? {
		return try {
			Words.select { Words.word like word.lowercase() }.map { it[Words.id].value }.firstOrNull()
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

	private fun registerMetrics() {
		Gauge.builder("crawler.urls.count") { getUrlsCount() }
			.description("Number of URLs processed")
			.register(meterRegistry)

		Gauge.builder("crawler.words.count") { getWordsCount() }
			.description("Number of words processed")
			.register(meterRegistry)

		Gauge.builder("crawler.wordLocations.count") { getWordLocationsCount() }
			.description("Number of word locations")
			.register(meterRegistry)

		Gauge.builder("crawler.links.count") { getLinksCount() }
			.description("Number of links processed")
			.register(meterRegistry)

		Gauge.builder("crawler.linkWords.count") { getLinkWordsCount() }
			.description("Number of link words")
			.register(meterRegistry)
	}

	private fun getUrlsCount() = transaction { Urls.selectAll().count().toDouble() }
	private fun getWordsCount() = transaction { Words.selectAll().count().toDouble() }
	private fun getWordLocationsCount() = transaction { WordLocations.selectAll().count().toDouble() }
	private fun getLinksCount() = transaction { Links.selectAll().count().toDouble() }
	private fun getLinkWordsCount() = transaction { LinkWords.selectAll().count().toDouble() }

	private companion object {
		val log: Logger = LoggerFactory.getLogger(Crawler::class.java)
		const val MAX_LINKS_COUNT = 50
	}
}