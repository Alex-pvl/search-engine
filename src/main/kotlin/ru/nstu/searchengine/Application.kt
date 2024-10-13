package ru.nstu.searchengine

import io.ktor.server.application.*
import ru.nstu.searchengine.plugins.configureRouting

fun main(args: Array<String>) {
	io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
	configureRouting()
}
