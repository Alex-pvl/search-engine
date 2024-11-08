package ru.nstu.searchengine.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.nstu.searchengine.searcher.Searcher

@Serializable
data class CalculatePageRankRequest(val iterations: Int)

@Serializable
data class QueryRequest(val query: String)

fun Route.searcherRoutes() {
	val searcher = Searcher()

	route("/searcher") {
		get("/match-rows") {
			val query = call.parameters["query"] ?:
				return@get call.respondText("Query parameter is missing", status = HttpStatusCode.BadRequest)
			val result = searcher.getMatchRows(query)
			call.respond(result)
		}
		get("/sorted-list") {
			val query = call.parameters["query"] ?:
				return@get call.respondText("Query parameter is missing", status = HttpStatusCode.BadRequest)
			val result = searcher.getSortedList(query)
			call.respond(result)
		}
		post("/page-rank") {
			val request = call.receive<CalculatePageRankRequest>()
			CoroutineScope(Dispatchers.IO).launch {
				searcher.calculatePageRank(request.iterations)
			}
			call.respond("Calculating PageRank started")
		}
		post("/highlight") {
			val request = call.receive<QueryRequest>()
			searcher.highlight(request.query)
			call.respond("OK")
		}

	}
}