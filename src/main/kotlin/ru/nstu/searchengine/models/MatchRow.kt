package ru.nstu.searchengine.models

import kotlinx.serialization.Serializable

@Serializable
data class MatchRow(
	val urlId: Int,
	val w0Location: Int,
	val w1Location: Int,
)