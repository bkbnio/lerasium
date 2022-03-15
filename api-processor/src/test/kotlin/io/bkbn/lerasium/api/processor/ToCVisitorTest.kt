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

class ToCVisitorTest : DescribeSpec({
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

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserToC.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.core.metadata.RequestInfo
        import io.bkbn.kompendium.core.metadata.ResponseInfo
        import io.bkbn.kompendium.core.metadata.method.DeleteInfo
        import io.bkbn.kompendium.core.metadata.method.GetInfo
        import io.bkbn.kompendium.core.metadata.method.PostInfo
        import io.bkbn.kompendium.core.metadata.method.PutInfo
        import io.bkbn.lerasium.api.model.GetByIdParams
        import io.bkbn.lerasium.api.model.PaginatedQuery
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.ktor.http.HttpStatusCode
        import kotlin.Unit
        import kotlin.collections.List

        public object UserToC {
          public val createUser: PostInfo<Unit, List<UserCreateRequest>, List<UserResponse>> =
              PostInfo<Unit, List<UserCreateRequest>, List<UserResponse>>(
            summary = "Create User",
            description = "Creates new User entities for the provided request objects",
            requestInfo = RequestInfo(
              description = "Details required to create new User entities",
            ),
            responseInfo = ResponseInfo(
              status = HttpStatusCode.Created,
              description = "The User entities were created successfully"
            ),
            tags = setOf("User")
          )


          public val countAllUser: GetInfo<Unit, CountResponse> = GetInfo<Unit, CountResponse>(
            summary = "Count User",
            description = "Counts total User entities",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "Successfully retrieved the total User entity count"
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


          public val getAllUser: GetInfo<PaginatedQuery, List<UserResponse>> =
              GetInfo<PaginatedQuery, List<UserResponse>>(
            summary = "Get All User",
            description =
                "Retrieves a collection of User entities, broken up by specified chunk and offset",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "Successfully retrieved the collection of User entities"
            ),
            tags = setOf("User")
          )

        }
        """.trimIndent()
      )
    }
    it("Can generate a table of contents with relational members") {
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
      result.kspGeneratedSources.first { it.name == "CountryToC.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.core.metadata.RequestInfo
        import io.bkbn.kompendium.core.metadata.ResponseInfo
        import io.bkbn.kompendium.core.metadata.method.DeleteInfo
        import io.bkbn.kompendium.core.metadata.method.GetInfo
        import io.bkbn.kompendium.core.metadata.method.PostInfo
        import io.bkbn.kompendium.core.metadata.method.PutInfo
        import io.bkbn.lerasium.api.model.GetByIdParams
        import io.bkbn.lerasium.api.model.PaginatedGetByIdQuery
        import io.bkbn.lerasium.api.model.PaginatedQuery
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.CountryCreateRequest
        import io.bkbn.lerasium.generated.models.CountryResponse
        import io.bkbn.lerasium.generated.models.CountryUpdateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.ktor.http.HttpStatusCode
        import kotlin.Unit
        import kotlin.collections.List

        public object CountryToC {
          public val createCountry: PostInfo<Unit, List<CountryCreateRequest>, List<CountryResponse>> =
              PostInfo<Unit, List<CountryCreateRequest>, List<CountryResponse>>(
            summary = "Create Country",
            description = "Creates new Country entities for the provided request objects",
            requestInfo = RequestInfo(
              description = "Details required to create new Country entities",
            ),
            responseInfo = ResponseInfo(
              status = HttpStatusCode.Created,
              description = "The Country entities were created successfully"
            ),
            tags = setOf("Country")
          )


          public val countAllCountry: GetInfo<Unit, CountResponse> = GetInfo<Unit, CountResponse>(
            summary = "Count Country",
            description = "Counts total Country entities",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "Successfully retrieved the total Country entity count"
            ),
            tags = setOf("Country")
          )


          public val getCountry: GetInfo<GetByIdParams, CountryResponse> =
              GetInfo<GetByIdParams, CountryResponse>(
            summary = "Get Country by ID",
            description = "Retrieves a Country by id",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "The Country was retrieved successfully"
            ),
            tags = setOf("Country")
          )


          public val updateCountry: PutInfo<GetByIdParams, CountryUpdateRequest, CountryResponse> =
              PutInfo<GetByIdParams, CountryUpdateRequest, CountryResponse>(
            summary = "Update Country",
            description = "Updates an existing Country",
            requestInfo = RequestInfo(
              description = "Takes an provided fields and overrides the corresponding Country info",
            ),
            responseInfo = ResponseInfo(
              status = HttpStatusCode.Created,
              description = "The Country was updated successfully"
            ),
            tags = setOf("Country")
          )


          public val deleteCountry: DeleteInfo<GetByIdParams, Unit> = DeleteInfo<GetByIdParams, Unit>(
            summary = "Delete Country by ID",
            description = "Deletes an existing Country",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.NoContent,
              description = "Successfully deleted Country"
            ),
            tags = setOf("Country")
          )


          public val getAllCountry: GetInfo<PaginatedQuery, List<CountryResponse>> =
              GetInfo<PaginatedQuery, List<CountryResponse>>(
            summary = "Get All Country",
            description =
                "Retrieves a collection of Country entities, broken up by specified chunk and offset",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "Successfully retrieved the collection of Country entities"
            ),
            tags = setOf("Country")
          )


          public val getCountryUser: GetInfo<PaginatedGetByIdQuery, List<UserResponse>> =
              GetInfo<PaginatedGetByIdQuery, List<UserResponse>>(
            summary = "Get Users",
            description = ""${'"'}
                |Retrieves a collection of User entities, broken up by specified
                |chunk and offset, referenced by the specified Country
                ""${'"'}.trimMargin(),
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "Successfully retrieved the collection of User entities"
            ),
            tags = setOf("Country")
          )

        }
        """.trimIndent()
      )
    }
    it("Can construct a table of contents with field queries") {
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
      result.kspGeneratedSources.first { it.name == "UserToC.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.core.metadata.RequestInfo
        import io.bkbn.kompendium.core.metadata.ResponseInfo
        import io.bkbn.kompendium.core.metadata.method.DeleteInfo
        import io.bkbn.kompendium.core.metadata.method.GetInfo
        import io.bkbn.kompendium.core.metadata.method.PostInfo
        import io.bkbn.kompendium.core.metadata.method.PutInfo
        import io.bkbn.lerasium.api.model.GetByIdParams
        import io.bkbn.lerasium.api.model.PaginatedQuery
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.ktor.http.HttpStatusCode
        import kotlin.Unit
        import kotlin.collections.List

        public object UserToC {
          public val createUser: PostInfo<Unit, List<UserCreateRequest>, List<UserResponse>> =
              PostInfo<Unit, List<UserCreateRequest>, List<UserResponse>>(
            summary = "Create User",
            description = "Creates new User entities for the provided request objects",
            requestInfo = RequestInfo(
              description = "Details required to create new User entities",
            ),
            responseInfo = ResponseInfo(
              status = HttpStatusCode.Created,
              description = "The User entities were created successfully"
            ),
            tags = setOf("User")
          )


          public val countAllUser: GetInfo<Unit, CountResponse> = GetInfo<Unit, CountResponse>(
            summary = "Count User",
            description = "Counts total User entities",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "Successfully retrieved the total User entity count"
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


          public val getAllUser: GetInfo<PaginatedQuery, List<UserResponse>> =
              GetInfo<PaginatedQuery, List<UserResponse>>(
            summary = "Get All User",
            description =
                "Retrieves a collection of User entities, broken up by specified chunk and offset",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "Successfully retrieved the collection of User entities"
            ),
            tags = setOf("User")
          )


          public val getByEmail: GetInfo<GetUserByEmailQuery, UserResponse> =
              GetInfo<GetUserByEmailQuery, UserResponse>(
            summary = "Get User by Email",
            description = "Retrieves a User entity by its email",
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "Successfully query"
            ),
            tags = setOf("User")
          )


          public val getByFirstName: GetInfo<GetUserByFirstNameQuery, List<UserResponse>> =
              GetInfo<GetUserByFirstNameQuery, List<UserResponse>>(
            summary = "Get User by FirstName",
            description = ""${'"'}
                |Retrieves a collection of User entities queried by firstName, broken up by specified
                |chunk and offset, referenced by the specified User
                ""${'"'}.trimMargin(),
            responseInfo = ResponseInfo(
              status = HttpStatusCode.OK,
              description = "Successfully query"
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
