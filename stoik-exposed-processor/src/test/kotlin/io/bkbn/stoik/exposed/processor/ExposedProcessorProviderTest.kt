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
  describe("Table Generation") {
    it("Can construct a simple Table with a single column") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Table("user")
        interface UserTableSpec {
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
        package io.bkbn.stoik.generated.table

        import java.util.UUID
        import kotlin.String
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object UserTable : UUIDTable("user") {
          public val name: Column<String> = varchar("name", 128)
        }

        public class UserEntity(
          id: EntityID<UUID>
        ) : UUIDEntity(id) {
          public var name: String by UserTable.name

          public companion object : UUIDEntityClass<UserEntity>(UserTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with an integer type column") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Table("counter")
        interface CounterTableSpec {
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
        package io.bkbn.stoik.generated.table

        import java.util.UUID
        import kotlin.Int
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object CounterTable : UUIDTable("counter") {
          public val count: Column<Int> = integer("count")
        }

        public class CounterEntity(
          id: EntityID<UUID>
        ) : UUIDEntity(id) {
          public var count: Int by CounterTable.count

          public companion object : UUIDEntityClass<CounterEntity>(CounterTable)
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
        package io.bkbn.stoik.generated.table

        import java.util.UUID
        import kotlin.Int
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object BigNameTable : UUIDTable("big_name") {
          public val superImportantField: Column<Int> = integer("super_important_field")
        }

        public class BigNameEntity(
          id: EntityID<UUID>
        ) : UUIDEntity(id) {
          public var superImportantField: Int by BigNameTable.superImportantField

          public companion object : UUIDEntityClass<BigNameEntity>(BigNameTable)
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
        package io.bkbn.stoik.generated.table

        import java.util.UUID
        import kotlin.Boolean
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object FactsTable : UUIDTable("facts") {
          public val isFact: Column<Boolean> = bool("isFact")
        }

        public class FactsEntity(
          id: EntityID<UUID>
        ) : UUIDEntity(id) {
          public var isFact: Boolean by FactsTable.isFact

          public companion object : UUIDEntityClass<FactsEntity>(FactsTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a long column type") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Table("big_num")
        interface BigNumTableSpec {
          val size: Long
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
        package io.bkbn.stoik.generated.table

        import java.util.UUID
        import kotlin.Long
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object BigNumTable : UUIDTable("big_num") {
          public val size: Column<Long> = long("size")
        }

        public class BigNumEntity(
          id: EntityID<UUID>
        ) : UUIDEntity(id) {
          public var size: Long by BigNumTable.size

          public companion object : UUIDEntityClass<BigNumEntity>(BigNumTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a float column type") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Table("floaty")
        interface FloatyTableSpec {
          @Column("pointy_num")
          val pointyNum: Float
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
        package io.bkbn.stoik.generated.table

        import java.util.UUID
        import kotlin.Float
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object FloatyTable : UUIDTable("floaty") {
          public val pointyNum: Column<Float> = float("pointy_num")
        }

        public class FloatyEntity(
          id: EntityID<UUID>
        ) : UUIDEntity(id) {
          public var pointyNum: Float by FloatyTable.pointyNum

          public companion object : UUIDEntityClass<FloatyEntity>(FloatyTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a varchar with a custom size") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.VarChar
        import io.bkbn.stoik.exposed.Table

        @Table("words")
        interface WordsTableSpec {
          @VarChar(size = 256)
          val word: String
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
        package io.bkbn.stoik.generated.table

        import java.util.UUID
        import kotlin.String
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column

        public object WordsTable : UUIDTable("words") {
          public val word: Column<String> = varchar("word", 256)
        }

        public class WordsEntity(
          id: EntityID<UUID>
        ) : UUIDEntity(id) {
          public var word: String by WordsTable.word

          public companion object : UUIDEntityClass<WordsEntity>(WordsTable)
        }
        """.trimIndent()
      )
    }
  }
  describe("File Creation") {
    it("Can construct multiple tables in a single source set") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        import io.bkbn.stoik.exposed.VarChar
        import io.bkbn.stoik.exposed.Table

        @Table("words")
        interface WordsTableSpec {
          @VarChar(size = 256)
          val word: String
        }

        @Table("other_words")
        interface OtherWordsTableSpec {
          @VarChar(size = 128)
          val wordy: String
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
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources[0].name shouldBe "OtherWordsTable.kt"
      result.kspGeneratedSources[1].name shouldBe "WordsTable.kt"
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
