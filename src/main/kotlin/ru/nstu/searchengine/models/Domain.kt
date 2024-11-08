package ru.nstu.searchengine.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Domains : IntIdTable("domains") {
	val domain = varchar("domain", 100).uniqueIndex()
}