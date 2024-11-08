package ru.nstu.searchengine.models

import org.jetbrains.exposed.dao.id.IntIdTable

object PageRanks : IntIdTable("pagerank") {
	val urlId = integer("url_id").references(Urls.id)
	val score = double("score")
}