package io.bkbn.lerasium.core.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.intellij.lang.annotations.Language
import java.io.File

class ModelProcessorProviderTest : DescribeSpec({
  describe("Validation") {
    // todo
  }
  describe("Basic Model Tests") {
    it("Can generate a file with create, update and response models") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.lerasium.core.Domain

          @Domain("User")
          interface UserDomain {
            val firstName: String
            val lastName: String
            val email: String
            val password: String
            val age: Int?
          }
        """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(ModelProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 1
      result.kspGeneratedSources.first().readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.models

        import io.bkbn.lerasium.core.model.Request
        import io.bkbn.lerasium.core.model.Response
        import io.bkbn.lerasium.core.serialization.Serializers
        import java.util.UUID
        import kotlin.Int
        import kotlin.String
        import kotlinx.datetime.LocalDateTime
        import kotlinx.serialization.Serializable

        @Serializable
        public data class UserCreateRequest(
          public val firstName: String,
          public val lastName: String,
          public val email: String,
          public val password: String,
          public val age: Int?
        ) : Request.Create

        @Serializable
        public data class UserUpdateRequest(
          public val firstName: String?,
          public val lastName: String?,
          public val email: String?,
          public val password: String?,
          public val age: Int?
        ) : Request.Update

        @Serializable
        public data class UserResponse(
          @Serializable(with = Serializers.Uuid::class)
          public val id: UUID,
          public val firstName: String,
          public val lastName: String,
          public val email: String,
          public val password: String,
          public val age: Int?,
          public val createdAt: LocalDateTime,
          public val updatedAt: LocalDateTime
        ) : Response
        """.trimIndent()
      )
    }
    it("Can generate models with nested domain models") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.lerasium.core.Domain

          @Domain("User")
          interface UserDomain {
            val email: String
            val metadata: UserMetadata
          }

          interface UserMetadata {
            val firstName: String
            val lastName: String
          }

        """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(ModelProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 1
      result.kspGeneratedSources.first().readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.models

        import io.bkbn.lerasium.core.model.Request
        import io.bkbn.lerasium.core.model.Response
        import io.bkbn.lerasium.core.serialization.Serializers
        import java.util.UUID
        import kotlin.String
        import kotlinx.datetime.LocalDateTime
        import kotlinx.serialization.Serializable

        @Serializable
        public data class UserCreateRequest(
          public val email: String,
          public val metadata: UserMetadataCreateRequest
        ) : Request.Create

        @Serializable
        public data class UserUpdateRequest(
          public val email: String?,
          public val metadata: UserMetadataUpdateRequest?
        ) : Request.Update

        @Serializable
        public data class UserResponse(
          @Serializable(with = Serializers.Uuid::class)
          public val id: UUID,
          public val email: String,
          public val metadata: UserMetadataResponse,
          public val createdAt: LocalDateTime,
          public val updatedAt: LocalDateTime
        ) : Response

        @Serializable
        public data class UserMetadataCreateRequest(
          public val firstName: String,
          public val lastName: String
        ) : Request.Create

        @Serializable
        public data class UserMetadataUpdateRequest(
          public val firstName: String?,
          public val lastName: String?
        ) : Request.Update

        @Serializable
        public data class UserMetadataResponse(
          public val firstName: String,
          public val lastName: String
        ) : Response
        """.trimIndent()
      )
    }
    it("Can support a domain with deeply nested models") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.lerasium.core.Domain

          @Domain("User")
          interface UserDomain {
            val email: String
            val metadata: UserMetadata
          }

          interface UserMetadata {
            val firstName: String
            val lastName: String
            val otherStuffs: OhBoiWeDeepInItNow
          }

          interface OhBoiWeDeepInItNow {
            val otherInfo: String
          }
        """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(ModelProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 1
      result.kspGeneratedSources.first().readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.models

        import io.bkbn.lerasium.core.model.Request
        import io.bkbn.lerasium.core.model.Response
        import io.bkbn.lerasium.core.serialization.Serializers
        import java.util.UUID
        import kotlin.String
        import kotlinx.datetime.LocalDateTime
        import kotlinx.serialization.Serializable

        @Serializable
        public data class UserCreateRequest(
          public val email: String,
          public val metadata: UserMetadataCreateRequest
        ) : Request.Create

        @Serializable
        public data class UserUpdateRequest(
          public val email: String?,
          public val metadata: UserMetadataUpdateRequest?
        ) : Request.Update

        @Serializable
        public data class UserResponse(
          @Serializable(with = Serializers.Uuid::class)
          public val id: UUID,
          public val email: String,
          public val metadata: UserMetadataResponse,
          public val createdAt: LocalDateTime,
          public val updatedAt: LocalDateTime
        ) : Response

        @Serializable
        public data class OhBoiWeDeepInItNowCreateRequest(
          public val otherInfo: String
        ) : Request.Create

        @Serializable
        public data class OhBoiWeDeepInItNowUpdateRequest(
          public val otherInfo: String?
        ) : Request.Update

        @Serializable
        public data class OhBoiWeDeepInItNowResponse(
          public val otherInfo: String
        ) : Response

        @Serializable
        public data class UserMetadataCreateRequest(
          public val firstName: String,
          public val lastName: String,
          public val otherStuffs: OhBoiWeDeepInItNowCreateRequest
        ) : Request.Create

        @Serializable
        public data class UserMetadataUpdateRequest(
          public val firstName: String?,
          public val lastName: String?,
          public val otherStuffs: OhBoiWeDeepInItNowUpdateRequest?
        ) : Request.Update

        @Serializable
        public data class UserMetadataResponse(
          public val firstName: String,
          public val lastName: String,
          public val otherStuffs: OhBoiWeDeepInItNowResponse
        ) : Response
        """.trimIndent()
      )
    }
  }
}) {
  companion object {
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

