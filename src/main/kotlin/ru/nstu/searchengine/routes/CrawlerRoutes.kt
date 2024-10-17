package ru.nstu.searchengine.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import ru.nstu.searchengine.crawler.Crawler
import java.util.concurrent.Executors

@Serializable
data class CrawlRequest(val urls: List<String>, val maxDepth: Int = 2)

@Serializable
data class HTMLRequest(val url: String)

@Serializable
data class LinkRequest(val url: String)

fun Route.crawlerRoutes() {
	val crawler = Crawler()
	val crawlerExecutor = Executors.newCachedThreadPool()
	val crawlerDispatcher = crawlerExecutor.asCoroutineDispatcher()
	val crawlerScope = CoroutineScope(crawlerDispatcher + SupervisorJob())

	environment.monitor.subscribe(ApplicationStopped) {
		crawlerExecutor.shutdown()
	}

	route("/crawler") {
		post("/start") {
			val request = call.receive<CrawlRequest>()
			crawlerScope.launch {
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
			call.respond(text ?: "lol")
		}
	}
}