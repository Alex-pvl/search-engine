package ru.nstu.searchengine.routes

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.nstu.searchengine.crawler.Crawler

@Serializable
data class CrawlRequest(val urls: List<String>, val maxDepth: Int = 2)

fun Route.crawlerRoutes(prometheusMeterRegistry: PrometheusMeterRegistry) {
	val crawler = Crawler(prometheusMeterRegistry)

	route("/crawler") {
		post("/start") {
			val request = call.receive<CrawlRequest>()
			CoroutineScope(Dispatchers.IO).launch {
				crawler.crawl(request.urls, request.maxDepth)
			}
			call.respondText("Crawling started")
		}

	}
}