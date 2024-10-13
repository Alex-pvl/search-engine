package ru.nstu.searchengine.db

import com.typesafe.config.ConfigFactory
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
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
			SchemaUtils.create(Urls, Words, WordLocations, Links, LinkWords)
		}
	}
}