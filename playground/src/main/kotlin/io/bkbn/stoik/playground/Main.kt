package io.bkbn.stoik.playground

import io.bkbn.stoik.generated.api.UserApi.userController
import io.bkbn.stoik.generated.table.UserEntity
import io.bkbn.stoik.generated.table.UserTable
import io.bkbn.stoik.playground.config.DatabaseConfig
import io.ktor.application.Application
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlin.random.Random
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
  val logger = logger("main")
  logger.info { "Initializing database and performing any necessary migrations" }
  DatabaseConfig.flyway.migrate()
  DatabaseConfig.relationalDatabase

  val test = transaction {
    UserEntity.new {
      firstName = "Ryan"
      lastName = "Brink-${Random.Default.nextInt()}"
      email = "$lastName@pm.me"
    }
  }

  logger.info { "Created new user: ${test.id}" }

  val otherTest = Testerino.readByEmail(test.email, test.firstName)

  logger.info { "Found you: ${otherTest.firstName} ${otherTest.lastName}" }

//  logger.info { "Launching API" }
//  EngineMain.main(args)
}

fun Application.module() {
  routing {
    route("/user") {
      userController()
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
