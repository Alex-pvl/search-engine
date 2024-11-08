package ru.nstu.searchengine.utils

import java.io.File
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

fun String.separateWords(): List<String> = this.lowercase().split(Regex(" "))
	.filter { it.isNotBlank() }

fun String.createMarkedHtmlFile(fileName: String, queryList: List<String>) {
	val wordList = this.splitWithIndex().map { it.value }.toList()
	val htmlContent = getMarkedHTML(wordList, queryList)
	File("src/main/resources/html/$fileName").writeText(htmlContent)
}

private fun getMarkedHTML(wordList: List<String>, queryList: List<String>): String {
	val highlightedWords = wordList.joinToString(" ") { word ->
		if (word.lowercase() in queryList.map { it.lowercase() }) {
			"<span style=\"background-color: yellow;\">$word</span>"
		} else {
			word
		}
	}

	// Оборачиваем текст в базовую HTML-структуру
	return """
            <html>
            <head>
                <title>Highlighted Search Results</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; }
                </style>
            </head>
            <body>
                <p>$highlightedWords</p>
            </body>
            </html>
        """.trimIndent()
}