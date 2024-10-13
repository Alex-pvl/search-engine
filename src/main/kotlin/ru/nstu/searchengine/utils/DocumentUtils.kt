package ru.nstu.searchengine.utils

import org.jsoup.nodes.Document

fun Document.getBodyAsText(): String = this.body().text()