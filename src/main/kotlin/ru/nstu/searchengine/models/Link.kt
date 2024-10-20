package ru.nstu.searchengine.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Links : IntIdTable("links") {
	val fromUrl = reference("from_url", Urls)
	val toUrl = reference("to_url", Urls)
}

data class Link(
	val id: Int,
	val fromUrlId: Int,
	val toUrlId: Int,
)