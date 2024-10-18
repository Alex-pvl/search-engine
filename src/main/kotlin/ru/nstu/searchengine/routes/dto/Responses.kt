package ru.nstu.searchengine.routes.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticResponse(
	val url: String,
	val statistics: Statistics,
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as StatisticResponse

		return url == other.url
	}

	override fun hashCode(): Int {
		var result = url.hashCode()
		result = 31 * result + statistics.hashCode()
		return result
	}
}

@Serializable
data class Statistics(
	val urlsCount: Long,
	val wordsCount: Long,
	val wordLocationsCount: Long,
	val linksCount: Long,
	val linkWordsCount: Long,
)