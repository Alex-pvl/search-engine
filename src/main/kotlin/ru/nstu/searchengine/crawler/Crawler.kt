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
import ru.nstu.searchengine.utils.extractDomain
import ru.nstu.searchengine.utils.getBodyAsText
import ru.nstu.searchengine.utils.isPreposition
import ru.nstu.searchengine.utils.splitWithIndex
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class Crawler(
	private val meterRegistry: PrometheusMeterRegistry,
) {
	private val dispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
	private val scope = CoroutineScope(dispatcher + SupervisorJob())
	private val indexedLinksCount = AtomicInteger(0)

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
				if (indexedLinksCount.get() >= MAX_LINKS_COUNT) {
					log.info("Max links count reached. Stopping.")
					jobs.forEach { it.cancel() }
					break
				}
				if (visitedUrls.add(url)) {
					val job = scope.launch {
						val newLinks = processUrl(url)
						if (newLinks != null) {
							synchronized(newUrls) {
								newUrls.addAll(newLinks)
							}
						}
					}
					jobs.add(job)
				}
			}

			jobs.joinAll()
			currentDepthUrls = newUrls.toList()
		}
		log.info("Crawling ended.")
	}

	private suspend fun processUrl(url: String): List<String>? {
		log.info("Start parsing link: $url")
		return try {
			val document = Jsoup.connect(url).get()
			val indexDeferred = CompletableDeferred<Unit>()
			scope.launch {
				addToIndex(document, url)
				indexDeferred.complete(Unit)
			}
			indexDeferred.await()
			parseLinks(document, url)
		} catch (e: Exception) {
			log.error("Error processing $url: ${e::class.simpleName}")
			null
		}
	}

	private suspend fun addToIndex(document: Document, url: String) = withContext(scope.coroutineContext) {
		indexedLinksCount.incrementAndGet()
		transaction {
			val domainId = getOrCreateDomain(url.extractDomain())
			val urlId = getOrCreateUrlId(url, domainId)
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

	private suspend fun parseLinks(document: Document, fromUrl: String): List<String> =
		withContext(scope.coroutineContext) {
			val links = mutableListOf<String>()
			transaction {
				val domainId = getOrCreateDomain(fromUrl.extractDomain())
				val fromUrlId = getOrCreateUrlId(fromUrl, domainId)
				val elements = document.getElementsByTag("a")

				for (element in elements) {
					val href = element.absUrl("href")

					if (href.isNotBlank() && href.startsWith("http")) {
						log.info("[$fromUrlId]: href=$href")

						val hrefDomainId = getOrCreateDomain(href.extractDomain())
						val toUrlId = getOrCreateUrlId(href, hrefDomainId)
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
			links
		}

	private fun getOrCreateUrlId(url: String, domainId: Int): Int {
		return Urls.select { Urls.url like url }.map { it[Urls.id].value }.firstOrNull()
			?: Urls.insertAndGetId {
				it[Urls.url] = url
				it[Urls.domainId] = domainId
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

	private fun getOrCreateDomain(domain: String): Int {
		return Domains.select { (Domains.domain like domain) }
			.map { it[Domains.id].value }
			.firstOrNull()
			?: Domains.insertAndGetId {
				it[this.domain] = domain
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

		Gauge.builder("crawler.indexedLinksCount.count") { getAtomic() }
			.description("Number of indexed links")
			.register(meterRegistry)
	}

	private fun getUrlsCount() = transaction { Urls.selectAll().count().toDouble() }
	private fun getWordsCount() = transaction { Words.selectAll().count().toDouble() }
	private fun getWordLocationsCount() = transaction { WordLocations.selectAll().count().toDouble() }
	private fun getLinksCount() = transaction { Links.selectAll().count().toDouble() }
	private fun getLinkWordsCount() = transaction { LinkWords.selectAll().count().toDouble() }
	private fun getAtomic() = indexedLinksCount.get().toDouble()

	private companion object {
		val log: Logger = LoggerFactory.getLogger(Crawler::class.java)
		const val MAX_LINKS_COUNT = 50
	}
}