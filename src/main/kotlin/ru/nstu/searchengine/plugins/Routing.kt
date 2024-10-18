package ru.nstu.searchengine.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import ru.nstu.searchengine.routes.crawlerRoutes

fun Application.configureRouting(prometheusMeterRegistry: PrometheusMeterRegistry) {
	routing {
		crawlerRoutes(prometheusMeterRegistry)
	}
}
