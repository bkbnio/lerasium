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
import io.bkbn.lerasium.generated.api.controller.OrganizationController.organizationHandler
import io.bkbn.lerasium.generated.api.controller.OrganizationRoleController.organizationRoleHandler
import io.bkbn.lerasium.generated.api.controller.ProjectController.projectHandler
import io.bkbn.lerasium.generated.api.controller.RepositoryController.repositoryHandler
import io.bkbn.lerasium.generated.api.controller.UserController.userHandler
import io.bkbn.lerasium.generated.persistence.config.PostgresConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.EngineMain
import io.ktor.server.routing.routing
import kotlinx.datetime.LocalDateTime
import org.apache.logging.log4j.kotlin.logger
import java.net.URI
import kotlin.reflect.typeOf

fun main(args: Array<String>) {
  val logger = logger("main")
  logger.info { "Performing migrations" }
  PostgresConfig.flyway.clean()
  PostgresConfig.flyway.migrate()

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
    organizationHandler()
    organizationRoleHandler()
    projectHandler()
    repositoryHandler()
  }
}
