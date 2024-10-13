package ru.nstu.searchengine.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import ru.nstu.searchengine.routes.crawlerRoutes

fun Application.configureRouting() {
	routing {
		crawlerRoutes()
	}
}
