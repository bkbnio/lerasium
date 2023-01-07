package io.bkbn.lerasium.playground.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoDatabase
import org.bson.UuidRepresentation
import org.litote.kmongo.KMongo

@Suppress("MagicNumber")
object DatabaseConfig {

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
