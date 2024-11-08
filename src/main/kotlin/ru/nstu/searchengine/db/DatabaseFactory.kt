package ru.nstu.searchengine.db

import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.nstu.searchengine.models.*

object DatabaseFactory {
	fun init() {
		val config = ConfigFactory.load().getConfig("database")
		val driverClassName = config.getString("driver")
		val jdbcURL = config.getString("url")
		val user = config.getString("user")
		val password = config.getString("password")

		Database.connect(
			url = jdbcURL,
			driver = driverClassName,
			user = user,
			password = password
		)

		transaction {
			SchemaUtils.create(Urls, Words, WordLocations, Links, LinkWords, PageRanks)
		}

		runBlocking { dropAndCreateIndexes() }
	}

	suspend fun dropAndCreateIndexes() {
		newSuspendedTransaction {
			exec("DROP INDEX IF EXISTS words_idx;")
			exec("DROP INDEX IF EXISTS urls_idx;")
			exec("DROP INDEX IF EXISTS word_locations_idx;")
			exec("DROP INDEX IF EXISTS links_to_idx;")
			exec("DROP INDEX IF EXISTS links_to_idx;")
			exec("CREATE INDEX IF NOT EXISTS words_idx ON words(word);")
			exec("CREATE INDEX IF NOT EXISTS urls_idx ON urls(url);")
			exec("CREATE INDEX IF NOT EXISTS word_locations_idx ON word_locations(word_id);")
			exec("CREATE INDEX IF NOT EXISTS links_to_idx ON links(to_url);")
			exec("CREATE INDEX IF NOT EXISTS links_to_idx ON links(from_url);")
			exec("DROP INDEX IF EXISTS pagerank_url_id_idx;")
			exec("CREATE INDEX IF NOT EXISTS pagerank_url_id_idx ON pagerank(url_id);")
			exec("REINDEX INDEX words_idx;")
			exec("REINDEX INDEX urls_idx;")
			exec("REINDEX INDEX word_locations_idx;")
			exec("REINDEX INDEX links_to_idx;")
			exec("REINDEX INDEX links_to_idx;")
			exec("REINDEX INDEX pagerank_url_id_idx;")
		}

	}
}