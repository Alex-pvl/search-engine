package ru.nstu.searchengine.routes

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nstu.searchengine.crawler.Crawler
import ru.nstu.searchengine.routes.dto.CrawlRequest

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