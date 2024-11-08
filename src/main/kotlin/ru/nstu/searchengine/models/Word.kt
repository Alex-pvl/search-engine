package ru.nstu.searchengine.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Words : IntIdTable("words") {
	val word = varchar("word", 255, ).uniqueIndex()
	val isIgnored = bool("is_ignored").default(false)
}