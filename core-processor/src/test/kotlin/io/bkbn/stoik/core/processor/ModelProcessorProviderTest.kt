package io.bkbn.stoik.core.processor

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

          import io.bkbn.stoik.core.Domain

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
        package io.bkbn.stoik.generated.models

        import kotlin.Int
        import kotlin.String

        public data class UserCreateRequest(
          public val firstName: String,
          public val lastName: String,
          public val email: String,
          public val password: String,
          public val age: Int?
        )

        public data class UserUpdateRequest(
          public val firstName: String?,
          public val lastName: String?,
          public val email: String?,
          public val password: String?,
          public val age: Int?
        )

        public data class UserResponse(
          public val firstName: String,
          public val lastName: String,
          public val email: String,
          public val password: String,
          public val age: Int?
        )
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

