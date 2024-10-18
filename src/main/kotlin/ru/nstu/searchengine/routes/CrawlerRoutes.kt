package ru.nstu.searchengine.routes

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nstu.searchengine.crawler.Crawler
import ru.nstu.searchengine.routes.dto.CrawlRequest
import ru.nstu.searchengine.routes.dto.HTMLRequest
import ru.nstu.searchengine.routes.dto.LinkRequest

fun Route.crawlerRoutes() {
	val crawler = Crawler()

	route("/crawler") {
		post("/start") {
			val request = call.receive<CrawlRequest>()
			CoroutineScope(Dispatchers.IO).launch {
				crawler.crawl(request.urls, request.maxDepth)
			}
			call.respondText("Crawling started")
		}
		post("/html") {
			val request = call.receive<HTMLRequest>()
			val text = crawler.getHtml(request.url)
			call.respondText(text)
		}
		post("/links") {
			val request = call.receive<LinkRequest>()
			val text = crawler.getLinks(request.url)
			call.respond(text ?: listOf())
		}
		get("/stats") {
			call.respond(crawler.getStatistics())
		}
		get("/json") {
			crawler.serializeJson()
			call.respond("done")
		}
	}
}