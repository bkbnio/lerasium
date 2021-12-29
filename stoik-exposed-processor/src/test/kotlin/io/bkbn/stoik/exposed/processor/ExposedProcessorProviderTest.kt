package io.bkbn.stoik.exposed.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.intellij.lang.annotations.Language
import java.io.File

class ExposedProcessorProviderTest : DescribeSpec({
  describe("Table Generator") {
    it("Can construct a simple Table file for a simple interface") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Demo.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Sensitive
        import io.bkbn.stoik.exposed.Table
        import io.bkbn.stoik.exposed.Unique

        sealed interface UserSpec {
          val firstName: String
          val lastName: String
          val email: String
          val password: String
        }

        @Table("user")
        interface UserTableSpec : UserSpec {
          @Column("first_name")
          override val firstName: String

          @Column("last_name")
          override val lastName: String

          @Unique
          override val email: String

          @Sensitive
          override val password: String
        }
      """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(ExposedProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 1
      result.kspGeneratedSources.first().readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.stoik.generated

        import kotlin.String
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object UserTable : UUIDTable("user") {
          public val firstName: Column<String> = varchar("firstName", 128)

          public val lastName: Column<String> = varchar("lastName", 128)

          public val email: Column<String> = varchar("email", 128)

          public val password: Column<String> = varchar("password", 128)
        }
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
