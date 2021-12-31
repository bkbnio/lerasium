package io.bkbn.stoik.dao.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.intellij.lang.annotations.Language
import java.io.File

class DaoProcessorTest : DescribeSpec({
  describe("Dao Generator") {
    it("Can construct a simple dao") {
      // arrange
      val sourceFile = SourceFile.kotlin("Spec.kt", """
        import io.bkbn.stoik.dao.core.Dao

        @Dao("User")
        interface UserDaoSpec
      """.trimIndent())

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(DaoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 1
      result.kspGeneratedSources.first().readTrimmed() shouldBe kotlinCode("""
        package io.bkbn.stoik.generated

        import kotlinx.serialization.Serializable

        public open class UserDao

        @Serializable
        public class CreateUserRequest

        @Serializable
        public class UpdateUserRequest

        @Serializable
        public class UserResponse
      """.trimIndent())
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

