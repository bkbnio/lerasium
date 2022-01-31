package io.bkbn.stoik.kmongo.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.intellij.lang.annotations.Language
import java.io.File

class KMongoProcessorProviderTest : DescribeSpec({
  describe("Document Generation") {
    it("Can generate a simple document") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDocument.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.stoik.generated.entity

        import io.bkbn.stoik.core.model.Entity
        import io.bkbn.stoik.generated.models.UserResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import kotlinx.serialization.Contextual
        import kotlinx.serialization.SerialName
        import kotlinx.serialization.Serializable

        @Serializable
        public data class UserEntity(
          @Contextual
          @SerialName("_id")
          public val id: UUID,
          public var name: String,
          public var createdAt: LocalDateTime,
          public var updatedAt: LocalDateTime
        ) : Entity<UserResponse> {
          public override fun toResponse(): UserResponse = with(::UserResponse) {
            val propertiesByName = UserEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                else -> propertiesByName[it.name]?.get(this@UserEntity)
              }
            }
            callBy(params)
          }
        }
        """.trimIndent()
      )
    }
  }
  describe("Dao Generation") {
    it("can build a simple crud dao") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.stoik.generated.entity

        import com.mongodb.client.MongoCollection
        import com.mongodb.client.MongoDatabase
        import io.bkbn.stoik.core.dao.Dao
        import io.bkbn.stoik.generated.models.UserCreateRequest
        import io.bkbn.stoik.generated.models.UserResponse
        import io.bkbn.stoik.generated.models.UserUpdateRequest
        import java.util.UUID
        import kotlin.Unit
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.litote.kmongo.deleteOneById
        import org.litote.kmongo.findOneById
        import org.litote.kmongo.getCollection
        import org.litote.kmongo.save

        public class UserDao(
          db: MongoDatabase
        ) : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
          private val collection: MongoCollection<UserEntity> = db.getCollection()

          public override fun create(request: UserCreateRequest): UserResponse {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entity = UserEntity(
                id = UUID.randomUUID(),
                name = request.name,
                createdAt = now,
                updatedAt = now
                )
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun read(id: UUID): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            return entity.toResponse()
          }

          public override fun update(id: UUID, request: UserUpdateRequest): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            request.name?.let {
              entity.name = it
            }
            entity.updatedAt = now
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun delete(id: UUID): Unit {
            collection.deleteOneById(id)
          }
        }
        """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
  }
}) {
  companion object {
    const val errorMessage = "\"\"Unable to get entity with id: \$id\"\""
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

    fun kotlinCode(
      @Language("kotlin") contents: String,
      postProcess: (String) -> String = { it }
    ): String = postProcess(contents)

    val sourceFile = SourceFile.kotlin(
      "Spec.kt", """
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.kmongo.Document

        @Domain("User")
        interface User {
          val name: String
        }

        @Document
        interface UserDoc : User
        """.trimIndent()
    )
  }
}
