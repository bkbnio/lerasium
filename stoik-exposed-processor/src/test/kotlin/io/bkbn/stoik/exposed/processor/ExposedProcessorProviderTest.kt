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
    it("Can construct a simple Table with a single column") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Table("user")
        interface UserTableSpec {
          @Column
          val name: String
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
          public val name: Column<String> = varchar("name", 128)
        }
        """.trimIndent()
      )
    }
    it("Can construct a Table with an integer type column") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Table("counter")
        interface CounterTableSpec {
          @Column
          val count: Int
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

        import kotlin.Int
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object CounterTable : UUIDTable("counter") {
          public val count: Column<Int> = integer("count")
        }
        """.trimIndent()
      )
    }
    it("Can override the column name") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Table("big_name")
        interface BigNameTableSpec {
          @Column("super_important_field")
          val superImportantField: Int
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

        import kotlin.Int
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object BigNameTable : UUIDTable("big_name") {
          public val superImportantField: Column<Int> = integer("super_important_field")
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a boolean column type") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Table("facts")
        interface FactTableSpec {
          @Column
          val isFact: Boolean
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

        import kotlin.Boolean
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object FactsTable : UUIDTable("facts") {
          public val isFact: Column<Boolean> = bool("isFact")
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
