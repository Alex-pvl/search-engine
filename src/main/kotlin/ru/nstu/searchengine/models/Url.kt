package ru.nstu.searchengine.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Urls : IntIdTable("urls") {
	val url = varchar("url", 2083).uniqueIndex()
	val domainId = reference("domain_id", Domains)
}