package io.bkbn.lerasium.playground

import io.bkbn.kompendium.core.Kompendium
import io.bkbn.kompendium.core.routes.redoc
import io.bkbn.kompendium.oas.OpenApiSpec
import io.bkbn.kompendium.oas.info.Contact
import io.bkbn.kompendium.oas.info.Info
import io.bkbn.kompendium.oas.info.License
import io.bkbn.kompendium.oas.schema.FormattedSchema
import io.bkbn.kompendium.oas.serialization.KompendiumSerializersModule
import io.bkbn.kompendium.oas.server.Server
import io.bkbn.lerasium.generated.api.AuthorApi.authorController
import io.bkbn.lerasium.generated.api.BookApi.bookController
import io.bkbn.lerasium.generated.api.BookReviewApi.bookReviewController
import io.bkbn.lerasium.generated.api.ProfileApi.profileController
import io.bkbn.lerasium.generated.api.UserApi.userController
import io.bkbn.lerasium.generated.entity.AuthorDao
import io.bkbn.lerasium.generated.entity.AuthorTable
import io.bkbn.lerasium.generated.entity.BookDao
import io.bkbn.lerasium.generated.entity.BookReviewDao
import io.bkbn.lerasium.generated.entity.BookReviewTable
import io.bkbn.lerasium.generated.entity.BookTable
import io.bkbn.lerasium.generated.entity.ProfileDao
import io.bkbn.lerasium.generated.entity.UserDao
import io.bkbn.lerasium.generated.entity.UserEntity
import io.bkbn.lerasium.generated.entity.UserTable
import io.bkbn.lerasium.playground.config.DatabaseConfig
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.netty.EngineMain
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

fun main(args: Array<String>) {
  val logger = logger("main")
  logger.info { "Initializing database and performing any necessary migrations" }
  DatabaseConfig.relationalDatabase
  transaction {
    val statements = SchemaUtils.createStatements(AuthorTable, BookTable, UserTable, BookReviewTable)
    println("-------------")
    statements.forEach { println(it.plus("\n")) }
    println("-------------")
  }
  DatabaseConfig.flyway.migrate()
  logger.info { "Launching API" }
  EngineMain.main(args)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
  install(ContentNegotiation) {
    json(Json {
      serializersModule = KompendiumSerializersModule.module
      encodeDefaults = true
      explicitNulls = false
    })
  }
  install(Kompendium) {
    addCustomTypeSchema(LocalDateTime::class, FormattedSchema(type = "string", format = "date-time"))
    spec = OpenApiSpec(
      info = Info(
        title = "Lerasium Playground API",
        version = "1.33.7",
        description = "Wow isn't this cool?",
        termsOfService = URI("https://example.com"),
        contact = Contact(
          name = "Homer Simpson",
          email = "chunkylover53@aol.com",
          url = URI("https://gph.is/1NPUDiM")
        ),
        license = License(
          name = "MIT",
          url = URI("https://github.com/bkbnio/kompendium/blob/main/LICENSE")
        )
      ),
      servers = mutableListOf(
        Server(
          url = URI("http://localhost:8080"),
          description = "Production instance of my API"
        ),
      )
    )
  }
  routing {
    redoc("The Playground")
    route("/") {
      userController(UserDao())
      bookController(BookDao())
      bookReviewController(BookReviewDao())
      authorController(AuthorDao())
      profileController(ProfileDao(DatabaseConfig.documentDatabase))
    }
  }
}

object Testerino {
  private val byFirstName: (String) -> Expression<Boolean> = { firstName -> UserTable.firstName eq firstName }
  private val byEmail: (String) -> Expression<Boolean> = { email -> UserTable.email eq email }
  private val composition: SqlExpressionBuilder.(String, String) -> Op<Boolean> =
    { email, firstName -> byFirstName(firstName) and byEmail(email) }

  fun readByEmail(email: String, firstName: String) = transaction {
    val users = UserEntity.find { composition(email, firstName) }
    users.first()
  }
}
