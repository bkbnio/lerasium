package io.bkbn.stoik.playground.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

@Suppress("MagicNumber")
object DatabaseConfig {

  private const val CONNECTION_URI = "jdbc:postgresql://localhost:5432/test_db"
  private const val POSTGRES_USER = "test_user"
  private const val POSTGRES_PASSWORD = "test_password"

  val flyway: Flyway by lazy {
    Flyway.configure()
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
}
