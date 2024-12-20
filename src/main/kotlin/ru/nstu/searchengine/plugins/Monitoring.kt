package ru.nstu.searchengine.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.configureMonitoring(): PrometheusMeterRegistry {
	val micrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

	install(MicrometerMetrics) {
		registry = micrometerRegistry
	}

	routing {
		get("/metrics") {
			call.respondText { micrometerRegistry.scrape() }
		}
	}

	return micrometerRegistry
}