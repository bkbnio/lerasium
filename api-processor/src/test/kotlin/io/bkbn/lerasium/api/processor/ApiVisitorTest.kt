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
      result.kspGeneratedSources shouldHaveSize 1
      result.kspGeneratedSources.first { it.name == "UserApi.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.core.metadata.DeleteInfo
        import io.bkbn.kompendium.core.metadata.GetInfo
        import io.bkbn.kompendium.core.metadata.PostInfo
        import io.bkbn.kompendium.core.metadata.PutInfo
        import io.bkbn.kompendium.core.plugin.NotarizedRoute
        import io.bkbn.lerasium.api.util.ApiDocumentationUtils.getAllParameters
        import io.bkbn.lerasium.api.util.ApiDocumentationUtils.idParameter
        import io.bkbn.lerasium.generated.entity.UserDao
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.ktor.http.HttpStatusCode
        import io.ktor.server.application.call
        import io.ktor.server.application.install
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

        public object UserApi {
          public fun Route.userController(dao: UserDao): Unit {
            route("/user") {
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

          private fun Route.rootDocumentation(): Unit {
            install(NotarizedRoute()) {
              tags = setOf("User")
              get = GetInfo.builder {
                summary("Get All User Entities")
                description("Retrieves a paginated list of User Entities")
                parameters(*getAllParameters().toTypedArray())
                response {
                  responseType<List<UserResponse>>()
                  responseCode(HttpStatusCode.OK)
                  description("Paginated list of User entities")
                }
              }
              post = PostInfo.builder {
                summary("Create New User Entity")
                description("Persists a new User entity in the database")
                response {
                  responseType<List<UserResponse>>()
                  responseCode(HttpStatusCode.Created)
                  description("User entities saved successfully")
                }
                request {
                  requestType<List<UserCreateRequest>>()
                  description("Collection of User entities the user wishes to persist")
                }
              }
            }
          }

          private fun Route.idDocumentation(): Unit {
            install(NotarizedRoute()) {
              tags = setOf("User")
              parameters = idParameter()
              get = GetInfo.builder {
                summary("Get User by ID")
                description("Retrieves the specified User entity by its ID")
                response {
                  responseType<UserResponse>()
                  responseCode(HttpStatusCode.OK)
                  description("The User entity with the specified ID")
                }
              }
              put = PutInfo.builder {
                summary("Update User by ID")
                description("Updates the specified User entity by its ID")
                request {
                  requestType<UserUpdateRequest>()
                  description("Fields that can be updated on the User entity")
                }
                response {
                  responseType<UserResponse>()
                  responseCode(HttpStatusCode.Created)
                  description("Indicates that the User entity was updated successfully")
                }
              }
              delete = DeleteInfo.builder {
                summary("Delete User by ID")
                description("Deletes the specified User entity by its ID")
                response {
                  responseType<Unit>()
                  responseCode(HttpStatusCode.NoContent)
                  description("Indicates that the User entity was deleted successfully")
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
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "CountryApi.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.core.metadata.DeleteInfo
        import io.bkbn.kompendium.core.metadata.GetInfo
        import io.bkbn.kompendium.core.metadata.PostInfo
        import io.bkbn.kompendium.core.metadata.PutInfo
        import io.bkbn.kompendium.core.plugin.NotarizedRoute
        import io.bkbn.lerasium.api.util.ApiDocumentationUtils.getAllParameters
        import io.bkbn.lerasium.api.util.ApiDocumentationUtils.idParameter
        import io.bkbn.lerasium.generated.entity.CountryDao
        import io.bkbn.lerasium.generated.models.CountryCreateRequest
        import io.bkbn.lerasium.generated.models.CountryResponse
        import io.bkbn.lerasium.generated.models.CountryUpdateRequest
        import io.ktor.http.HttpStatusCode
        import io.ktor.server.application.call
        import io.ktor.server.application.install
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

        public object CountryApi {
          public fun Route.countryController(dao: CountryDao): Unit {
            route("/country") {
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
                  installUsersDocumentation()
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

          private fun Route.rootDocumentation(): Unit {
            install(NotarizedRoute()) {
              tags = setOf("Country")
              get = GetInfo.builder {
                summary("Get All Country Entities")
                description("Retrieves a paginated list of Country Entities")
                parameters(*getAllParameters().toTypedArray())
                response {
                  responseType<List<CountryResponse>>()
                  responseCode(HttpStatusCode.OK)
                  description("Paginated list of Country entities")
                }
              }
              post = PostInfo.builder {
                summary("Create New Country Entity")
                description("Persists a new Country entity in the database")
                response {
                  responseType<List<CountryResponse>>()
                  responseCode(HttpStatusCode.Created)
                  description("Country entities saved successfully")
                }
                request {
                  requestType<List<CountryCreateRequest>>()
                  description("Collection of Country entities the user wishes to persist")
                }
              }
            }
          }

          private fun Route.idDocumentation(): Unit {
            install(NotarizedRoute()) {
              tags = setOf("Country")
              parameters = idParameter()
              get = GetInfo.builder {
                summary("Get Country by ID")
                description("Retrieves the specified Country entity by its ID")
                response {
                  responseType<CountryResponse>()
                  responseCode(HttpStatusCode.OK)
                  description("The Country entity with the specified ID")
                }
              }
              put = PutInfo.builder {
                summary("Update Country by ID")
                description("Updates the specified Country entity by its ID")
                request {
                  requestType<CountryUpdateRequest>()
                  description("Fields that can be updated on the Country entity")
                }
                response {
                  responseType<CountryResponse>()
                  responseCode(HttpStatusCode.Created)
                  description("Indicates that the Country entity was updated successfully")
                }
              }
              delete = DeleteInfo.builder {
                summary("Delete Country by ID")
                description("Deletes the specified Country entity by its ID")
                response {
                  responseType<Unit>()
                  responseCode(HttpStatusCode.NoContent)
                  description("Indicates that the Country entity was deleted successfully")
                }
              }
            }
          }

          private fun Route.installUsersDocumentation(): Unit {
            install(NotarizedRoute()) {
              tags = setOf("Country")
              get = GetInfo.builder {
                summary("Get All Country Users")
                description(""${'"'}
                    |Retrieves a paginated list of Users entities associated
                    |with the provided Country
                    ""${'"'}.trimMargin())
                parameters(*getAllParameters().toTypedArray().plus(idParameter()))
                response {
                  responseType<List<CountryResponse>>()
                  responseCode(HttpStatusCode.OK)
                  description("Paginated list of Country entities")
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
      result.kspGeneratedSources shouldHaveSize 1
      result.kspGeneratedSources.first { it.name == "UserApi.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.core.metadata.DeleteInfo
        import io.bkbn.kompendium.core.metadata.GetInfo
        import io.bkbn.kompendium.core.metadata.PostInfo
        import io.bkbn.kompendium.core.metadata.PutInfo
        import io.bkbn.kompendium.core.plugin.NotarizedRoute
        import io.bkbn.lerasium.api.util.ApiDocumentationUtils.getAllParameters
        import io.bkbn.lerasium.api.util.ApiDocumentationUtils.idParameter
        import io.bkbn.lerasium.generated.entity.UserDao
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.ktor.http.HttpStatusCode
        import io.ktor.server.application.call
        import io.ktor.server.application.install
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

        public object UserApi {
          public fun Route.userController(dao: UserDao): Unit {
            route("/user") {
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
              route("/email/{email}") {
                installEmailQueryDocumentation()
                `get` {
                  val email = call.parameters["email"]!!
                  val result = dao.getByEmail(email)
                  call.respond(result)
                }
              }
              route("/firstName/{firstName}") {
                installFirstNameQueryDocumentation()
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

          private fun Route.rootDocumentation(): Unit {
            install(NotarizedRoute()) {
              tags = setOf("User")
              get = GetInfo.builder {
                summary("Get All User Entities")
                description("Retrieves a paginated list of User Entities")
                parameters(*getAllParameters().toTypedArray())
                response {
                  responseType<List<UserResponse>>()
                  responseCode(HttpStatusCode.OK)
                  description("Paginated list of User entities")
                }
              }
              post = PostInfo.builder {
                summary("Create New User Entity")
                description("Persists a new User entity in the database")
                response {
                  responseType<List<UserResponse>>()
                  responseCode(HttpStatusCode.Created)
                  description("User entities saved successfully")
                }
                request {
                  requestType<List<UserCreateRequest>>()
                  description("Collection of User entities the user wishes to persist")
                }
              }
            }
          }

          private fun Route.idDocumentation(): Unit {
            install(NotarizedRoute()) {
              tags = setOf("User")
              parameters = idParameter()
              get = GetInfo.builder {
                summary("Get User by ID")
                description("Retrieves the specified User entity by its ID")
                response {
                  responseType<UserResponse>()
                  responseCode(HttpStatusCode.OK)
                  description("The User entity with the specified ID")
                }
              }
              put = PutInfo.builder {
                summary("Update User by ID")
                description("Updates the specified User entity by its ID")
                request {
                  requestType<UserUpdateRequest>()
                  description("Fields that can be updated on the User entity")
                }
                response {
                  responseType<UserResponse>()
                  responseCode(HttpStatusCode.Created)
                  description("Indicates that the User entity was updated successfully")
                }
              }
              delete = DeleteInfo.builder {
                summary("Delete User by ID")
                description("Deletes the specified User entity by its ID")
                response {
                  responseType<Unit>()
                  responseCode(HttpStatusCode.NoContent)
                  description("Indicates that the User entity was deleted successfully")
                }
              }
            }
          }

          private fun Route.installEmailQueryDocumentation(): Unit {
            install(NotarizedRoute()) {
              tags = setOf("User")
              get = GetInfo.builder {
                summary("Get User by Email")
                description(""${'"'}
                    |Attempts to find a User entity associated
                    |with the provided Email id
                    ""${'"'}.trimMargin())
                parameters(*idParameter().toTypedArray())
                response {
                  responseType<UserResponse>()
                  responseCode(HttpStatusCode.OK)
                  description("User entity associated with the specified email")
                }
              }
            }
          }

          private fun Route.installFirstNameQueryDocumentation(): Unit {
            install(NotarizedRoute()) {
              tags = setOf("User")
              get = GetInfo.builder {
                summary("Get All User by FirstName")
                description(""${'"'}
                    |Attempts to find all User entities associated
                    |with the provided FirstName id
                    ""${'"'}.trimMargin())
                parameters(*getAllParameters().toTypedArray().plus(idParameter()))
                response {
                  responseType<List<UserResponse>>()
                  responseCode(HttpStatusCode.OK)
                  description("User entities associated with the specified firstName")
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
