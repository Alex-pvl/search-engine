package ru.nstu.searchengine.models

import org.jetbrains.exposed.dao.id.IntIdTable

object LinkWords : IntIdTable("link_words") {
	val word = reference("word_id", Words)
	val link = reference("link_id", Links)
}

data class LinkWord(
	val id: Int,
	val wordId: Int,
	val linkId: Int
)