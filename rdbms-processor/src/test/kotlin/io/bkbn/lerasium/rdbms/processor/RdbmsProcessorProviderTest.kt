package io.bkbn.lerasium.rdbms.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.bkbn.lerasium.utils.TestUtils.errorMessage
import io.bkbn.lerasium.utils.TestUtils.kotlinCode
import io.bkbn.lerasium.utils.TestUtils.kspGeneratedSources
import io.bkbn.lerasium.utils.TestUtils.readTrimmed
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RdbmsProcessorProviderTest : DescribeSpec({
  describe("Dao Generation") {
    it("Can create a simple dao") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.Table

        @Domain("User")
        interface User {
          val firstName: String
          val lastName: String
        }

        @Table
        interface UserTable : User
        """.trimIndent()
      )
      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.persistence.dao

        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.bkbn.lerasium.generated.persistence.entity.UserEntity
        import java.util.UUID
        import kotlin.Int
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.jetbrains.exposed.sql.transactions.transaction

        public object UserDao : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
          public override fun create(requests: List<UserCreateRequest>): List<UserResponse> = transaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entities = requests.map { request ->
              transaction {
                UserEntity.new {
                  firstName = request.firstName
                  lastName = request.lastName
                  createdAt = now
                  updatedAt = now
                }
              }
            }
            entities.map { it.toResponse() }
          }

          public override fun read(id: UUID): UserResponse = transaction {
            val entity = UserEntity.findById(id) ?: error("PLACEHOLDER")
            entity.toResponse()
          }

          public override fun update(id: UUID, request: UserUpdateRequest): UserResponse = transaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entity = UserEntity.findById(id) ?: error("PLACEHOLDER")
            request.firstName?.let {
              entity.firstName = it
            }
            request.lastName?.let {
              entity.lastName = it
            }
            entity.updatedAt = now
            entity.toResponse()
          }

          public override fun delete(id: UUID) = transaction {
            val entity = UserEntity.findById(id) ?: error("PLACEHOLDER")
            entity.delete()
          }

          public override fun countAll(): CountResponse = transaction {
            val count = UserEntity.count()
            CountResponse(count)
          }

          public override fun getAll(chunk: Int, offset: Int): List<UserResponse> = transaction {
            val entities = UserEntity.all().limit(chunk, offset.toLong())
            entities.map { entity ->
              entity.toResponse()
            }
          }
        }
        """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
    it("Can create a dao with a one-to-many reference ") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import java.util.UUID
        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.core.Relation
        import io.bkbn.lerasium.rdbms.ForeignKey
        import io.bkbn.lerasium.rdbms.OneToMany
        import io.bkbn.lerasium.rdbms.Table

        @Domain("Country")
        interface Country {
          val name: String
          @Relation
          val users: User
        }

        @Table
        interface CountryTable : Country {
          @OneToMany("country")
          override val users: User
        }

        @Domain("User")
        interface User {
          val name: String
          val country: Country
        }

        @Table
        interface UserTable : User {
          @ForeignKey
          override val country: Country
        }
      """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 4
      result.kspGeneratedSources.first { it.name == "CountryDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.persistence.dao

        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.CountryCreateRequest
        import io.bkbn.lerasium.generated.models.CountryResponse
        import io.bkbn.lerasium.generated.models.CountryUpdateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.persistence.entity.CountryEntity
        import java.util.UUID
        import kotlin.Int
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.jetbrains.exposed.sql.transactions.transaction

        public object CountryDao :
            Dao<CountryEntity, CountryResponse, CountryCreateRequest, CountryUpdateRequest> {
          public override fun create(requests: List<CountryCreateRequest>): List<CountryResponse> =
              transaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entities = requests.map { request ->
              transaction {
                CountryEntity.new {
                  name = request.name
                  createdAt = now
                  updatedAt = now
                }
              }
            }
            entities.map { it.toResponse() }
          }

          public override fun read(id: UUID): CountryResponse = transaction {
            val entity = CountryEntity.findById(id) ?: error("PLACEHOLDER")
            entity.toResponse()
          }

          public override fun update(id: UUID, request: CountryUpdateRequest): CountryResponse =
              transaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entity = CountryEntity.findById(id) ?: error("PLACEHOLDER")
            request.name?.let {
              entity.name = it
            }
            entity.updatedAt = now
            entity.toResponse()
          }

          public override fun delete(id: UUID) = transaction {
            val entity = CountryEntity.findById(id) ?: error("PLACEHOLDER")
            entity.delete()
          }

          public override fun countAll(): CountResponse = transaction {
            val count = CountryEntity.count()
            CountResponse(count)
          }

          public override fun getAll(chunk: Int, offset: Int): List<CountryResponse> = transaction {
            val entities = CountryEntity.all().limit(chunk, offset.toLong())
            entities.map { entity ->
              entity.toResponse()
            }
          }

          public fun getAllUsers(
            id: UUID,
            chunk: Int,
            offset: Int,
          ): List<UserResponse> = transaction {
            val entity = CountryEntity[id]
            entity.users.limit(chunk, offset.toLong()).toList().map { it.toResponse() }
          }
        }
        """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
    it("Builds the correct index accessors") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.lerasium.core.Domain
          import io.bkbn.lerasium.persistence.Index
          import io.bkbn.lerasium.rdbms.Table

          @Domain("User")
          interface User {
            val email: String
            val firstName: String
          }

          @Table
          internal interface UserTable : User {
            @Index(true)
            override val email: String

            @Index
            override val favoriteFood: String?
          }
        """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.persistence.dao

        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import io.bkbn.lerasium.generated.persistence.entity.UserEntity
        import io.bkbn.lerasium.generated.persistence.entity.UserTable
        import java.util.UUID
        import kotlin.Int
        import kotlin.String
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.jetbrains.exposed.sql.transactions.transaction

        public object UserDao : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
          public override fun create(requests: List<UserCreateRequest>): List<UserResponse> = transaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entities = requests.map { request ->
              transaction {
                UserEntity.new {
                  email = request.email
                  favoriteFood = request.favoriteFood
                  firstName = request.firstName
                  createdAt = now
                  updatedAt = now
                }
              }
            }
            entities.map { it.toResponse() }
          }

          public override fun read(id: UUID): UserResponse = transaction {
            val entity = UserEntity.findById(id) ?: error("PLACEHOLDER")
            entity.toResponse()
          }

          public override fun update(id: UUID, request: UserUpdateRequest): UserResponse = transaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entity = UserEntity.findById(id) ?: error("PLACEHOLDER")
            request.email?.let {
              entity.email = it
            }
            request.favoriteFood?.let {
              entity.favoriteFood = it
            }
            request.firstName?.let {
              entity.firstName = it
            }
            entity.updatedAt = now
            entity.toResponse()
          }

          public override fun delete(id: UUID) = transaction {
            val entity = UserEntity.findById(id) ?: error("PLACEHOLDER")
            entity.delete()
          }

          public override fun countAll(): CountResponse = transaction {
            val count = UserEntity.count()
            CountResponse(count)
          }

          public override fun getAll(chunk: Int, offset: Int): List<UserResponse> = transaction {
            val entities = UserEntity.all().limit(chunk, offset.toLong())
            entities.map { entity ->
              entity.toResponse()
            }
          }

          public fun getByEmail(email: String): UserResponse = transaction {
            UserEntity.find { UserTable.email eq email }.first().toResponse()
          }

          public fun getByFavoriteFood(
            favoriteFood: String?,
            chunk: Int,
            offset: Int,
          ): List<UserResponse> = transaction {
            UserEntity.find { UserTable.favoriteFood eq favoriteFood }.limit(chunk, offset.toLong()).map {
                it.toResponse() }
          }
        }
        """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
  }
})
