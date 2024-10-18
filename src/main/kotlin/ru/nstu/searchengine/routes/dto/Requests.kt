package ru.nstu.searchengine.routes.dto

import kotlinx.serialization.Serializable

@Serializable
data class CrawlRequest(
	val urls: List<String>,
	val maxDepth: Int = 2,
)

@Serializable
data class HTMLRequest(val url: String)

@Serializable
data class LinkRequest(val url: String)