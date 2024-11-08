package ru.nstu.searchengine.utils

import java.net.URL

private val ignoreWords = setOf(
	"без", "безо",
	"близ",
	"в", "во",
	"вместо",
	"вне",
	"для",
	"до",
	"за",
	"из", "изо",
	"из-за",
	"из-под",
	"к", "ко",
	"кроме",
	"между", "меж",
	"на",
	"над", "надо",
	"о", "об", "обо",
	"от", "ото",
	"перед", "передо", "пред", "предо",
	"по",
	"под", "подо",
	"при",
	"про",
	"ради",
	"с", "со",
	"сквозь",
	"среди",
	"у",
	"через", "чрез",
)

fun String.isPreposition() = this in ignoreWords

fun String.splitWithIndex() = Regex("[A-Za-zА-Яа-яЁё0-9]+")
	.findAll(this)
	.map { it.value }
	.withIndex()

fun String.extractDomain(): String = URL(this).host
