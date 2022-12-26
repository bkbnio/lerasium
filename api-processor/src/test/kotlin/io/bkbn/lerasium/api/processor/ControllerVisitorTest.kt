package io.bkbn.lerasium.api.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.bkbn.lerasium.utils.TestUtils
import io.bkbn.lerasium.utils.TestUtils.kspGeneratedSources
import io.bkbn.lerasium.utils.TestUtils.readTrimmed
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldInclude

class ControllerVisitorTest : DescribeSpec({
  describe("Validation") {
    it("Throws error when domain is not provided") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.lerasium.api.Api

          @Api
          interface UserApiSpec
        """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(KtorProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
      result.messages shouldInclude "Must implement an interface annotated with Domain"
    }
    it("Throws error in event of invalid domain") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.lerasium.core.Domain
          import io.bkbn.lerasium.api.Api

          @Domain("user")
          interface UserDomain

          @Api
          interface UserApiSpec : UserDomain
        """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(KtorProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
      result.messages shouldInclude "Domain is invalid"
    }
  }
  describe("Controller Generation") {
    it("Can build an API with simple CRUD functionality") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(simpleSourceFile)
        symbolProcessorProviders = listOf(KtorProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 4
      result.kspGeneratedSources.first { it.name == "UserController.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api.controller

        import io.bkbn.lerasium.generated.api.docs.UserDocumentation.idDocumentation
        import io.bkbn.lerasium.generated.api.docs.UserDocumentation.rootDocumentation
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.bkbn.lerasium.generated.persistence.dao.UserDao
        import io.ktor.http.HttpStatusCode
        import io.ktor.server.application.call
        import io.ktor.server.request.receive
        import io.ktor.server.response.respond
        import io.ktor.server.routing.Route
        import io.ktor.server.routing.`get`
        import io.ktor.server.routing.delete
        import io.ktor.server.routing.post
        import io.ktor.server.routing.put
        import io.ktor.server.routing.route
        import java.util.UUID
        import kotlin.Unit
        import kotlin.collections.List

        public object UserController {
          public fun Route.userHandler(dao: UserDao): Unit {
            route("/user") {
              rootRoute(dao)
              idRoute(dao)
            }
          }

          private fun Route.rootRoute(dao: UserDao): Unit {
            rootDocumentation()
            post {
              val request = call.receive<List<UserCreateRequest>>()
              val result = dao.create(request)
              call.respond(result)
            }
            `get` {
              val chunk = call.parameters["chunk"]?.toInt() ?: 100
              val offset = call.parameters["offset"]?.toInt() ?: 0
              val result = dao.getAll(chunk, offset)
              call.respond(result)
            }
          }

          private fun Route.idRoute(dao: UserDao): Unit {
            route("/{id}") {
              idDocumentation()
              `get` {
                val id = UUID.fromString(call.parameters["id"])
                val result = dao.read(id)
                call.respond(result)
              }
              put {
                val id = UUID.fromString(call.parameters["id"])
                val request = call.receive<UserUpdateRequest>()
                val result = dao.update(id, request)
                call.respond(result)
              }
              delete {
                val id = UUID.fromString(call.parameters["id"])
                dao.delete(id)
                call.respond(HttpStatusCode.NoContent)
              }
            }
          }
        }
        """.trimIndent()
      )
    }
    it("Can build a route to access a relational member") {
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.api.Api
        import java.util.UUID
        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.core.Relation

        @Domain("Country")
        interface Country {
          val name: String
          @Relation
          val users: User
        }

        @Api
        interface CountryApi : Country

        @Domain("User")
        interface User {
          val name: String
          val country: Country
        }

        @Api
        interface UserApi : User
      """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(KtorProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 7
      result.kspGeneratedSources.first { it.name == "CountryController.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api.controller

        import io.bkbn.lerasium.generated.api.docs.CountryDocumentation.idDocumentation
        import io.bkbn.lerasium.generated.api.docs.CountryDocumentation.rootDocumentation
        import io.bkbn.lerasium.generated.api.docs.CountryDocumentation.usersRelationDocumentation
        import io.bkbn.lerasium.generated.models.CountryCreateRequest
        import io.bkbn.lerasium.generated.models.CountryUpdateRequest
        import io.bkbn.lerasium.generated.persistence.dao.CountryDao
        import io.ktor.http.HttpStatusCode
        import io.ktor.server.application.call
        import io.ktor.server.request.receive
        import io.ktor.server.response.respond
        import io.ktor.server.routing.Route
        import io.ktor.server.routing.`get`
        import io.ktor.server.routing.delete
        import io.ktor.server.routing.post
        import io.ktor.server.routing.put
        import io.ktor.server.routing.route
        import java.util.UUID
        import kotlin.Unit
        import kotlin.collections.List

        public object CountryController {
          public fun Route.countryHandler(dao: CountryDao): Unit {
            route("/country") {
              rootRoute(dao)
              idRoute(dao)
            }
          }

          private fun Route.rootRoute(dao: CountryDao): Unit {
            rootDocumentation()
            post {
              val request = call.receive<List<CountryCreateRequest>>()
              val result = dao.create(request)
              call.respond(result)
            }
            `get` {
              val chunk = call.parameters["chunk"]?.toInt() ?: 100
              val offset = call.parameters["offset"]?.toInt() ?: 0
              val result = dao.getAll(chunk, offset)
              call.respond(result)
            }
          }

          private fun Route.idRoute(dao: CountryDao): Unit {
            route("/{id}") {
              idDocumentation()
              `get` {
                val id = UUID.fromString(call.parameters["id"])
                val result = dao.read(id)
                call.respond(result)
              }
              put {
                val id = UUID.fromString(call.parameters["id"])
                val request = call.receive<CountryUpdateRequest>()
                val result = dao.update(id, request)
                call.respond(result)
              }
              delete {
                val id = UUID.fromString(call.parameters["id"])
                dao.delete(id)
                call.respond(HttpStatusCode.NoContent)
              }
              route("/users") {
                usersRelationDocumentation()
                `get` {
                  val id = UUID.fromString(call.parameters["id"])
                  val chunk = call.parameters["chunk"]?.toInt() ?: 100
                  val offset = call.parameters["offset"]?.toInt() ?: 0
                  val result = dao.getAllUsers(id, chunk, offset)
                  call.respond(result)
                }
              }
            }
          }
        }
        """.trimIndent()
      )
    }
    it("Can build routes for getBy queries") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.lerasium.api.Api
          import io.bkbn.lerasium.api.GetBy
          import io.bkbn.lerasium.core.Domain

          @Domain("User")
          interface UserDomain {
            val email: String
            val firstName: String
          }

          @Api("User")
          interface UserApiSpec : UserDomain {
            @GetBy(true)
            override val email: String

            @GetBy
            override val firstName: String
          }
        """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(KtorProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 4
      result.kspGeneratedSources.first { it.name == "UserController.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api.controller

        import io.bkbn.lerasium.generated.api.docs.UserDocumentation.emailQueryDocumentation
        import io.bkbn.lerasium.generated.api.docs.UserDocumentation.firstNameQueryDocumentation
        import io.bkbn.lerasium.generated.api.docs.UserDocumentation.idDocumentation
        import io.bkbn.lerasium.generated.api.docs.UserDocumentation.rootDocumentation
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.bkbn.lerasium.generated.persistence.dao.UserDao
        import io.ktor.http.HttpStatusCode
        import io.ktor.server.application.call
        import io.ktor.server.request.receive
        import io.ktor.server.response.respond
        import io.ktor.server.routing.Route
        import io.ktor.server.routing.`get`
        import io.ktor.server.routing.delete
        import io.ktor.server.routing.post
        import io.ktor.server.routing.put
        import io.ktor.server.routing.route
        import java.util.UUID
        import kotlin.Unit
        import kotlin.collections.List

        public object UserController {
          public fun Route.userHandler(dao: UserDao): Unit {
            route("/user") {
              rootRoute(dao)
              idRoute(dao)
              queryRoutes(dao)
            }
          }

          private fun Route.rootRoute(dao: UserDao): Unit {
            rootDocumentation()
            post {
              val request = call.receive<List<UserCreateRequest>>()
              val result = dao.create(request)
              call.respond(result)
            }
            `get` {
              val chunk = call.parameters["chunk"]?.toInt() ?: 100
              val offset = call.parameters["offset"]?.toInt() ?: 0
              val result = dao.getAll(chunk, offset)
              call.respond(result)
            }
          }

          private fun Route.idRoute(dao: UserDao): Unit {
            route("/{id}") {
              idDocumentation()
              `get` {
                val id = UUID.fromString(call.parameters["id"])
                val result = dao.read(id)
                call.respond(result)
              }
              put {
                val id = UUID.fromString(call.parameters["id"])
                val request = call.receive<UserUpdateRequest>()
                val result = dao.update(id, request)
                call.respond(result)
              }
              delete {
                val id = UUID.fromString(call.parameters["id"])
                dao.delete(id)
                call.respond(HttpStatusCode.NoContent)
              }
            }
          }

          private fun Route.queryRoutes(dao: UserDao): Unit {
            route("/email/{email}") {
              emailQueryDocumentation()
              `get` {
                val email = call.parameters["email"]!!
                val result = dao.getByEmail(email)
                call.respond(result)
              }
            }
            route("/firstName/{firstName}") {
              firstNameQueryDocumentation()
              `get` {
                val firstName = call.parameters["firstName"]!!
                val chunk = call.parameters["chunk"]?.toInt() ?: 100
                val offset = call.parameters["offset"]?.toInt() ?: 0
                val result = dao.getByFirstName(firstName, chunk, offset)
                call.respond(result)
              }
            }
          }
        }
        """.trimIndent()
      )
    }
  }
}) {
  companion object {
    private val simpleSourceFile = SourceFile.kotlin(
      "Spec.kt", """
          package test

          import io.bkbn.lerasium.api.Api
          import io.bkbn.lerasium.core.Domain

          @Domain("User")
          interface UserDomain

          @Api("User")
          interface UserApiSpec : UserDomain
        """.trimIndent()
    )
  }
}
