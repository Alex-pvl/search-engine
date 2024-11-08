package ru.nstu.searchengine.models

import org.jetbrains.exposed.dao.id.IntIdTable

object WordLocations : IntIdTable("word_locations") {
	val url = reference("url_id", Urls)
	val word = reference("word_id", Words)
	val location = integer("location").index("word_locations_location_idx")
}