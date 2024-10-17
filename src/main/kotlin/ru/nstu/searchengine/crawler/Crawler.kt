package ru.nstu.searchengine.crawler

import ru.nstu.searchengine.models.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.nstu.searchengine.utils.getBodyAsText
import ru.nstu.searchengine.utils.isPreposition
import ru.nstu.searchengine.utils.splitWithIndex

class Crawler {
	suspend fun crawl(urls: List<String>, maxDepth: Int = 2) {
		val visitedUrls = mutableSetOf<String>()
		var currentDepthUrls = urls

		for (depth in 1..maxDepth) {
			val newUrls = mutableSetOf<String>()
			val jobs = mutableListOf<Job>()

			for (url in currentDepthUrls) {
				if (url !in visitedUrls) {
					visitedUrls.add(url)
					val job = CoroutineScope(Dispatchers.IO).launch {
						processUrl(url)?.let { foundUrls ->
							newUrls.addAll(foundUrls)
						}
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

	suspend fun getLinks(url: String) = processUrl(url)

	private suspend fun processUrl(url: String): List<String>? {
		return try {
			withContext(Dispatchers.IO) {
				val document = Jsoup.connect(url).get()
				addToIndex(document, url)
				parseLinks(document, url)
			}
		} catch (e: Exception) {
			println("Error processing $url: ${e.message}")
			null
		}
	}

	private fun addToIndex(document: Document, url: String) {
		transaction {
			val urlId = getOrCreateUrlId(url)
			val text = document.getBodyAsText()

			for ((location, word) in text.splitWithIndex()) {
				val isIgnored = word.isPreposition()
				val wordId = getOrCreateWordId(word, isIgnored)
				WordLocations.insert {
					it[this.url] = urlId
					it[this.word] = wordId
					it[this.location] = location
				}
			}
		}
	}

	private suspend fun parseLinks(document: Document, fromUrl: String): List<String> {
		val links = mutableListOf<String>()
		withContext(Dispatchers.IO) {
			transaction {
				val fromUrlId = getOrCreateUrlId(fromUrl)
				log.info("fromUrlId: $fromUrlId")

				val elements = document.getElementsByTag("a")
				log.info("elements: $elements")

				for ((visited, element) in elements.withIndex()) {
					val href = element.attr("abs:href")
					if (href.isNotBlank() && visited < MAX_LINKS_COUNT) {
						log.info("href: $href")

						val toUrlId = getOrCreateUrlId(href)
						val linkId = getOrCreateLinkId(fromUrlId, toUrlId)
						val linkText = element.text()

						for ((_, word) in linkText.splitWithIndex()) {
							val isIgnored = word.isPreposition()
							val wordId = getOrCreateWordId(word, isIgnored)
							LinkWords.insert {
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
		return Urls.select { Urls.url eq url }.map { it[Urls.id].value }.firstOrNull()
			?: Urls.insertAndGetId {
				it[Urls.url] = url
			}.value
	}

	private fun getOrCreateWordId(word: String, isIgnored: Boolean): Int {
		return Words.select { Words.word eq word.lowercase() }.map { it[Words.id].value }.firstOrNull()
			?: Words.insertAndGetId {
				it[Words.word] = word.lowercase()
				it[Words.isIgnored] = isIgnored
			}.value
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