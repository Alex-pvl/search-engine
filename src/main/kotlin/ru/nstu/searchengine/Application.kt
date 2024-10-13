package ru.nstu.searchengine

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ru.nstu.searchengine.db.DatabaseFactory
import ru.nstu.searchengine.plugins.configureMonitoring
import ru.nstu.searchengine.plugins.configureRouting
import ru.nstu.searchengine.plugins.configureSerialization

fun main() {
	DatabaseFactory.init()

	val config = HoconApplicationConfig(ConfigFactory.load())
	val host = config.propertyOrNull("ktor.deployment.host")?.getString() ?: "0.0.0.0"
	val port = config.propertyOrNull("ktor.deployment.port")?.getString()?.toInt() ?: 8080

	embeddedServer(Netty, port = port, host = host) {
		module()
	}.start(wait = true)
}

fun Application.module() {
	configureSerialization()
	configureMonitoring()
	configureRouting()
}
