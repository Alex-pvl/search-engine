package ru.nstu.searchengine.searcher

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.nstu.searchengine.db.DatabaseFactory
import ru.nstu.searchengine.models.*
import ru.nstu.searchengine.utils.createMarkedHtmlFile
import ru.nstu.searchengine.utils.getBodyAsText
import ru.nstu.searchengine.utils.separateWords

class Searcher {
	fun getMatchRows(query: String): List<MatchRow> {
		val words = query.separateWords()
		if (words.size < 2) return emptyList()

		val wordIds = getWordIds(query)
		if (wordIds.size < 2) return emptyList()

		val w0 = WordLocations.alias("w0")
		val w1 = WordLocations.alias("w1")

		return transaction {
			w0.innerJoin(w1, onColumn = { w0[WordLocations.url] }, otherColumn = { w1[WordLocations.url] })
				.slice(w0[WordLocations.url], w0[WordLocations.location], w1[WordLocations.location])
				.select {
					(w0[WordLocations.word] eq wordIds[0]) and
						(w1[WordLocations.word] eq wordIds[1])
				}
				.map {
					MatchRow(
						it[w0[WordLocations.url]].value,
						it[w0[WordLocations.location]],
						it[w1[WordLocations.location]]
					)
				}
		}
	}

	// Функция 5: получение отсортированного списка URL
	fun getSortedList(queryString: String): List<Pair<String, Double>> {
		val rows = getMatchRows(queryString)
		val frequencyScores = frequencyScore(rows)

		return frequencyScores.toList()
			.sortedByDescending { it.second }
			.take(50)
			.map { getUrlName(it.first)!! to it.second }
	}

	// Функция 7: вычисление PageRank
	suspend fun calculatePageRank(iterations: Int = 5) {
		newSuspendedTransaction {
			SchemaUtils.drop(PageRanks)
			SchemaUtils.create(PageRanks)

			DatabaseFactory.dropAndCreateIndexes()

			Urls.selectAll().forEach { row ->
				PageRanks.insert {
					it[urlId] = row[Urls.id].value
					it[score] = 1.0
				}
			}

			repeat(iterations) { iteration ->
				log.info("Iteration $iteration started")

				PageRanks.selectAll().forEach { row ->
					val urlId = row[PageRanks.urlId]
					var pr = 0.15

					Links.select { Links.toUrl eq urlId }.forEach { linkRow ->
						val linkingUrlId = linkRow[Links.fromUrl].value
						val linkingPr = PageRanks.select { PageRanks.urlId eq linkingUrlId }
							.single()[PageRanks.score]
						val linkingCount = Links.select { Links.fromUrl eq linkingUrlId }.count()

						pr += 0.85 * (linkingPr / linkingCount)
					}

					PageRanks.update({ PageRanks.urlId eq urlId }) {
						it[score] = pr
					}
				}
			}
		}
		log.info("calculatePageRank: iterations=$iterations ended")
	}

	fun highlight(query: String) {
		val list = getSortedList(query)
		val queryList = query.separateWords()

		list.forEach {
			val url = it.first
			val document = Jsoup.connect(url).get()
			val text = document.getBodyAsText()
			text.createMarkedHtmlFile("highlighted_${it.first.hashCode()}.html", queryList)
		}
	}

	private fun normalizeScores(scores: Map<Int, Double>, smallIsBetter: Boolean = false): Map<Int, Double> {
		val minScore = scores.values.minOrNull() ?: 0.0
		val maxScore = scores.values.maxOrNull() ?: 1.0
		val range = maxScore - minScore

		return scores.mapValues { (_, score) ->
			if (smallIsBetter) minScore / (score + 1e-6) else (score - minScore) / range
		}
	}

	// Функция 4: вычисление частоты слов
	private fun frequencyScore(rows: List<MatchRow>): Map<Int, Double> {
		val counts = rows.groupingBy { it.urlId }.eachCount().mapValues { it.value.toDouble() }
		return normalizeScores(counts, smallIsBetter = false)
	}

	private fun getUrlName(urlId: Int): String? {
		return transaction {
			Urls.select { Urls.id eq urlId }
				.singleOrNull()?.get(Urls.url)
		}
	}

	private fun getWordIds(query: String): List<Int> {
		val words = query.separateWords()
		return transaction {
			words.mapNotNull {
				Words.select { Words.word eq it }
					.firstOrNull()
					?.get(Words.id)?.value
			}
		}
	}

	private companion object {
		val log: Logger = LoggerFactory.getLogger(Searcher::class.java)
	}
}