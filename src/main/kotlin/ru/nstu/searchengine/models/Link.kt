package ru.nstu.searchengine.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Links : IntIdTable("links") {
	val fromUrl = reference("from_url", Urls).index("links_from_url_idx")
	val toUrl = reference("to_url", Urls).index("links_to_url_idx")
}