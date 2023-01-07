package io.bkbn.lerasium.playground

import io.bkbn.kompendium.core.plugin.NotarizedApplication
import io.bkbn.kompendium.core.routes.redoc
import io.bkbn.kompendium.json.schema.definition.TypeDefinition
import io.bkbn.kompendium.oas.OpenApiSpec
import io.bkbn.kompendium.oas.info.Contact
import io.bkbn.kompendium.oas.info.Info
import io.bkbn.kompendium.oas.info.License
import io.bkbn.kompendium.oas.server.Server
import io.bkbn.lerasium.generated.api.config.lerasiumConfig
import io.bkbn.lerasium.generated.api.controller.AuthorController.authorHandler
import io.bkbn.lerasium.generated.api.controller.BookController.bookHandler
import io.bkbn.lerasium.generated.api.controller.BookReviewController.bookReviewHandler
import io.bkbn.lerasium.generated.api.controller.ProfileController.profileHandler
import io.bkbn.lerasium.generated.api.controller.UserController.userHandler
import io.bkbn.lerasium.generated.persistence.config.PostgresConfig
import io.bkbn.lerasium.generated.persistence.table.AuthorTable
import io.bkbn.lerasium.generated.persistence.table.BookReviewTable
import io.bkbn.lerasium.generated.persistence.table.BookTable
import io.bkbn.lerasium.generated.persistence.table.UserEntity
import io.bkbn.lerasium.generated.persistence.table.UserTable
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.EngineMain
import io.ktor.server.routing.routing
import kotlinx.datetime.LocalDateTime
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI
import kotlin.reflect.typeOf

fun main(args: Array<String>) {
  val logger = logger("main")
  logger.info { "Initializing database and performing any necessary migrations" }
  PostgresConfig.relationalDatabase
  PostgresConfig.flyway.clean()

  transaction {
    val statements = SchemaUtils.createStatements(AuthorTable, BookTable, UserTable, BookReviewTable)
    println("-------------")
    statements.forEach { println(it.plus("\n")) }
    println("-------------")
  }

  PostgresConfig.flyway.migrate()

  // Inject some dummy data
  // TODO

  logger.info { "Launching API" }
  EngineMain.main(args)
}

fun Application.module() {
  lerasiumConfig()
  install(NotarizedApplication()) {
    customTypes = mapOf(
      typeOf<LocalDateTime>() to TypeDefinition(type = "string", format = "date-time")
    )
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
    userHandler()
    bookHandler()
    bookReviewHandler()
    authorHandler()
    profileHandler()
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
