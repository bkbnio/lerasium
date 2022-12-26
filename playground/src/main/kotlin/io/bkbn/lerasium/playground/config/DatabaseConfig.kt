package io.bkbn.lerasium.playground.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoDatabase
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bson.UuidRepresentation
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.litote.kmongo.KMongo

@Suppress("MagicNumber")
object DatabaseConfig {

  private const val CONNECTION_URI = "jdbc:postgresql://localhost:5432/test_db"
  private const val POSTGRES_USER = "test_user"
  private const val POSTGRES_PASSWORD = "test_password"

  val flyway: Flyway by lazy {
    Flyway.configure()
      .cleanDisabled(false)
      .dataSource(CONNECTION_URI, POSTGRES_USER, POSTGRES_PASSWORD)
      .locations("db/migration")
      .load() ?: error("Problem Loading Flyway!! Please verify Database Connection / Migration Info")
  }

  val relationalDatabase: Database by lazy {
    Database.connect(HikariDataSource(HikariConfig().apply {
      jdbcUrl = CONNECTION_URI
      username = POSTGRES_USER
      password = POSTGRES_PASSWORD
      maximumPoolSize = 5
      initializationFailTimeout = 60000L
      isAutoCommit = false
      transactionIsolation = "TRANSACTION_REPEATABLE_READ"
      validate()
    }))
  }

  val documentDatabase: MongoDatabase by lazy {
    val clientSettings = MongoClientSettings
      .builder()
      .apply {
        applyConnectionString(ConnectionString("mongodb://test_user:test_password@localhost:27017"))
        uuidRepresentation(UuidRepresentation.STANDARD)
      }
      .build()
    val mongoClient = KMongo.createClient(clientSettings)
    mongoClient.getDatabase("test_db")
  }
}
