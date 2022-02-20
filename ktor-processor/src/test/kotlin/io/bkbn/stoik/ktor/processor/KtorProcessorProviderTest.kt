package io.bkbn.stoik.ktor.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldInclude
import org.intellij.lang.annotations.Language
import java.io.File

class KtorProcessorProviderTest : DescribeSpec({
  describe("Validation") {
    it("Throws error when domain is not provided") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.stoik.ktor.Api

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

          import io.bkbn.stoik.core.Domain
          import io.bkbn.stoik.ktor.Api

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
  describe("API Crud Functionality") {
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
      result.kspGeneratedSources.first { it.name == "UserApi.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.stoik.generated.api

        import io.bkbn.kompendium.core.Notarized.notarizedDelete
        import io.bkbn.kompendium.core.Notarized.notarizedGet
        import io.bkbn.kompendium.core.Notarized.notarizedPost
        import io.bkbn.kompendium.core.Notarized.notarizedPut
        import io.bkbn.stoik.generated.api.UserToC.createUser
        import io.bkbn.stoik.generated.api.UserToC.deleteUser
        import io.bkbn.stoik.generated.api.UserToC.getUser
        import io.bkbn.stoik.generated.api.UserToC.updateUser
        import io.bkbn.stoik.generated.entity.UserDao
        import io.bkbn.stoik.generated.models.UserCreateRequest
        import io.bkbn.stoik.generated.models.UserUpdateRequest
        import io.ktor.application.call
        import io.ktor.http.HttpStatusCode
        import io.ktor.request.receive
        import io.ktor.response.respond
        import io.ktor.routing.Route
        import io.ktor.routing.route
        import java.util.UUID
        import kotlin.Unit

        public object UserApi {
          public fun Route.userController(dao: UserDao): Unit {
            route("/user") {
              notarizedPost(createUser) {
                val request = call.receive<UserCreateRequest>()
                val result = dao.create(request)
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
            }
          }
        }
        """.trimIndent()
      )
    }
  }
  describe("Table of Contents") {
    it("Can generate a simple table of contents") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(simpleSourceFile)
        symbolProcessorProviders = listOf(KtorProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserToC.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.stoik.generated.api

        import io.bkbn.kompendium.core.metadata.RequestInfo
        import io.bkbn.kompendium.core.metadata.ResponseInfo
        import io.bkbn.kompendium.core.metadata.method.DeleteInfo
        import io.bkbn.kompendium.core.metadata.method.GetInfo
        import io.bkbn.kompendium.core.metadata.method.PostInfo
        import io.bkbn.kompendium.core.metadata.method.PutInfo
        import io.bkbn.stoik.generated.models.UserCreateRequest
        import io.bkbn.stoik.generated.models.UserResponse
        import io.bkbn.stoik.generated.models.UserUpdateRequest
        import io.bkbn.stoik.ktor.model.GetByIdParams
        import io.ktor.http.HttpStatusCode
        import kotlin.Unit

        public object UserToC {
          public val createUser: PostInfo<Unit, UserCreateRequest, UserResponse> =
              PostInfo<Unit, UserCreateRequest, UserResponse>(
            summary = "Create User",
            description = "Creates a new User",
            requestInfo = RequestInfo(
              description = "Details required to create a new User",
            ),
            responseInfo = ResponseInfo(
              status = HttpStatusCode.Created,
              description = "The User was retrieved successfully"
            ),
            tags = setOf("User")
          )


          public val getUser: GetInfo<GetByIdParams, UserResponse> = GetInfo<GetByIdParams, UserResponse>(
            summary = "Get User by ID",
            description = "Retrieves a User by id",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "The User was retrieved successfully"
            ),
            tags = setOf("User")
          )


          public val updateUser: PutInfo<GetByIdParams, UserUpdateRequest, UserResponse> =
              PutInfo<GetByIdParams, UserUpdateRequest, UserResponse>(
            summary = "Update User",
            description = "Updates an existing User",
            requestInfo = RequestInfo(
              description = "Takes an provided fields and overrides the corresponding User info",
            ),
            responseInfo = ResponseInfo(
              status = HttpStatusCode.Created,
              description = "The User was updated successfully"
            ),
            tags = setOf("User")
          )


          public val deleteUser: DeleteInfo<GetByIdParams, Unit> = DeleteInfo<GetByIdParams, Unit>(
            summary = "Delete User by ID",
            description = "Deletes an existing User",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.NoContent,
              description = "Successfully deleted User"
            ),
            tags = setOf("User")
          )

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

          import io.bkbn.stoik.ktor.Api
          import io.bkbn.stoik.core.Domain

          @Domain("User")
          interface UserDomain

          @Api("User")
          interface UserApiSpec : UserDomain
        """.trimIndent()
    )

    private val KotlinCompilation.Result.workingDir: File
      get() =
        outputDirectory.parentFile!!

    val KotlinCompilation.Result.kspGeneratedSources: List<File>
      get() {
        val kspWorkingDir = workingDir.resolve("ksp")
        val kspGeneratedDir = kspWorkingDir.resolve("sources")
        val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
        return kotlinGeneratedDir.walkTopDown().toList().filter { it.isFile }
      }

    fun File.readTrimmed() = readText().trim()

    fun kotlinCode(@Language("kotlin") contents: String): String = contents
  }
}

