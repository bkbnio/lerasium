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

class ApiVisitorTest : DescribeSpec({
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
  describe("API Routes") {
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
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserApi.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.core.Notarized.notarizedDelete
        import io.bkbn.kompendium.core.Notarized.notarizedGet
        import io.bkbn.kompendium.core.Notarized.notarizedPost
        import io.bkbn.kompendium.core.Notarized.notarizedPut
        import io.bkbn.lerasium.generated.api.UserToC.countAllUser
        import io.bkbn.lerasium.generated.api.UserToC.createUser
        import io.bkbn.lerasium.generated.api.UserToC.deleteUser
        import io.bkbn.lerasium.generated.api.UserToC.getAllUser
        import io.bkbn.lerasium.generated.api.UserToC.getUser
        import io.bkbn.lerasium.generated.api.UserToC.updateUser
        import io.bkbn.lerasium.generated.entity.UserDao
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.ktor.application.call
        import io.ktor.http.HttpStatusCode
        import io.ktor.request.receive
        import io.ktor.response.respond
        import io.ktor.routing.Route
        import io.ktor.routing.route
        import java.util.UUID
        import kotlin.Unit
        import kotlin.collections.List

        public object UserApi {
          public fun Route.userController(dao: UserDao): Unit {
            route("/user") {
              notarizedPost(createUser) {
                val request = call.receive<List<UserCreateRequest>>()
                val result = dao.create(request)
                call.respond(result)
              }
              notarizedGet(getAllUser) {
                val chunk = call.parameters["chunk"]?.toInt() ?: 100
                val offset = call.parameters["offset"]?.toInt() ?: 0
                val result = dao.getAll(chunk, offset)
                call.respond(result)
              }
              route("/{id}") {
                notarizedGet(getUser) {
                  val id = UUID.fromString(call.parameters["id"])
                  val result = dao.read(id)
                  call.respond(result)
                }
                notarizedPut(updateUser) {
                  val id = UUID.fromString(call.parameters["id"])
                  val request = call.receive<UserUpdateRequest>()
                  val result = dao.update(id, request)
                  call.respond(result)
                }
                notarizedDelete(deleteUser) {
                  val id = UUID.fromString(call.parameters["id"])
                  dao.delete(id)
                  call.respond(HttpStatusCode.NoContent)
                }
              }
              route("/count") {
                notarizedGet(countAllUser) {
                  val result = dao.countAll()
                  call.respond(result)
                }
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
      result.kspGeneratedSources shouldHaveSize 4
      result.kspGeneratedSources.first { it.name == "CountryApi.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.core.Notarized.notarizedDelete
        import io.bkbn.kompendium.core.Notarized.notarizedGet
        import io.bkbn.kompendium.core.Notarized.notarizedPost
        import io.bkbn.kompendium.core.Notarized.notarizedPut
        import io.bkbn.lerasium.generated.api.CountryToC.countAllCountry
        import io.bkbn.lerasium.generated.api.CountryToC.createCountry
        import io.bkbn.lerasium.generated.api.CountryToC.deleteCountry
        import io.bkbn.lerasium.generated.api.CountryToC.getAllCountry
        import io.bkbn.lerasium.generated.api.CountryToC.getCountry
        import io.bkbn.lerasium.generated.api.CountryToC.getCountryUser
        import io.bkbn.lerasium.generated.api.CountryToC.updateCountry
        import io.bkbn.lerasium.generated.entity.CountryDao
        import io.bkbn.lerasium.generated.models.CountryCreateRequest
        import io.bkbn.lerasium.generated.models.CountryUpdateRequest
        import io.ktor.application.call
        import io.ktor.http.HttpStatusCode
        import io.ktor.request.receive
        import io.ktor.response.respond
        import io.ktor.routing.Route
        import io.ktor.routing.route
        import java.util.UUID
        import kotlin.Unit
        import kotlin.collections.List

        public object CountryApi {
          public fun Route.countryController(dao: CountryDao): Unit {
            route("/country") {
              notarizedPost(createCountry) {
                val request = call.receive<List<CountryCreateRequest>>()
                val result = dao.create(request)
                call.respond(result)
              }
              notarizedGet(getAllCountry) {
                val chunk = call.parameters["chunk"]?.toInt() ?: 100
                val offset = call.parameters["offset"]?.toInt() ?: 0
                val result = dao.getAll(chunk, offset)
                call.respond(result)
              }
              route("/{id}") {
                notarizedGet(getCountry) {
                  val id = UUID.fromString(call.parameters["id"])
                  val result = dao.read(id)
                  call.respond(result)
                }
                notarizedPut(updateCountry) {
                  val id = UUID.fromString(call.parameters["id"])
                  val request = call.receive<CountryUpdateRequest>()
                  val result = dao.update(id, request)
                  call.respond(result)
                }
                notarizedDelete(deleteCountry) {
                  val id = UUID.fromString(call.parameters["id"])
                  dao.delete(id)
                  call.respond(HttpStatusCode.NoContent)
                }
                route("/users") {
                  notarizedGet(getCountryUser) {
                    val id = UUID.fromString(call.parameters["id"])
                    val chunk = call.parameters["chunk"]?.toInt() ?: 100
                    val offset = call.parameters["offset"]?.toInt() ?: 0
                    val result = dao.getAllUsers(id, chunk, offset)
                    call.respond(result)
                  }
                }
              }
              route("/count") {
                notarizedGet(countAllCountry) {
                  val result = dao.countAll()
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
      result.kspGeneratedSources shouldHaveSize 3
      result.kspGeneratedSources.first { it.name == "UserApi.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.core.Notarized.notarizedDelete
        import io.bkbn.kompendium.core.Notarized.notarizedGet
        import io.bkbn.kompendium.core.Notarized.notarizedPost
        import io.bkbn.kompendium.core.Notarized.notarizedPut
        import io.bkbn.lerasium.generated.api.UserToC.countAllUser
        import io.bkbn.lerasium.generated.api.UserToC.createUser
        import io.bkbn.lerasium.generated.api.UserToC.deleteUser
        import io.bkbn.lerasium.generated.api.UserToC.getAllUser
        import io.bkbn.lerasium.generated.api.UserToC.getByEmail
        import io.bkbn.lerasium.generated.api.UserToC.getByFirstName
        import io.bkbn.lerasium.generated.api.UserToC.getUser
        import io.bkbn.lerasium.generated.api.UserToC.updateUser
        import io.bkbn.lerasium.generated.entity.UserDao
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.ktor.application.call
        import io.ktor.http.HttpStatusCode
        import io.ktor.request.receive
        import io.ktor.response.respond
        import io.ktor.routing.Route
        import io.ktor.routing.route
        import java.util.UUID
        import kotlin.Unit
        import kotlin.collections.List

        public object UserApi {
          public fun Route.userController(dao: UserDao): Unit {
            route("/user") {
              notarizedPost(createUser) {
                val request = call.receive<List<UserCreateRequest>>()
                val result = dao.create(request)
                call.respond(result)
              }
              notarizedGet(getAllUser) {
                val chunk = call.parameters["chunk"]?.toInt() ?: 100
                val offset = call.parameters["offset"]?.toInt() ?: 0
                val result = dao.getAll(chunk, offset)
                call.respond(result)
              }
              route("/{id}") {
                notarizedGet(getUser) {
                  val id = UUID.fromString(call.parameters["id"])
                  val result = dao.read(id)
                  call.respond(result)
                }
                notarizedPut(updateUser) {
                  val id = UUID.fromString(call.parameters["id"])
                  val request = call.receive<UserUpdateRequest>()
                  val result = dao.update(id, request)
                  call.respond(result)
                }
                notarizedDelete(deleteUser) {
                  val id = UUID.fromString(call.parameters["id"])
                  dao.delete(id)
                  call.respond(HttpStatusCode.NoContent)
                }
              }
              route("/count") {
                notarizedGet(countAllUser) {
                  val result = dao.countAll()
                  call.respond(result)
                }
              }
              route("/email/{email}") {
                notarizedGet(getByEmail) {
                  val email = call.parameters["email"]!!
                  val result = dao.getByEmail(email)
                  call.respond(result)
                }
              }
              route("/firstName/{firstName}") {
                notarizedGet(getByFirstName) {
                  val firstName = call.parameters["firstName"]!!
                  val chunk = call.parameters["chunk"]?.toInt() ?: 100
                  val offset = call.parameters["offset"]?.toInt() ?: 0
                  val result = dao.getByFirstName(firstName, chunk, offset)
                  call.respond(result)
                }
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
