package io.bkbn.stoik.exposed.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
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
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.exposed.Table

        @Domain("User")
        interface User {
          val name: String
        }

        @Table
        interface UserTable : User
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
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserTable.kt" }.readTrimmed() shouldBe kotlinCode(
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
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.exposed.Table

        @Domain("Counter")
        interface Counter {
          val count: Int
        }

        @Table
        interface CounterTable : Counter
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
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "CounterTable.kt" }.readTrimmed() shouldBe kotlinCode(
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
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Domain("User")
        interface User {
          val userInfo: String
        }

        @Table
        interface UserTable : User {
          @Column("super_important_field")
          override val userInfo: String
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
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserTable.kt" }.readTrimmed() shouldBe kotlinCode(
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
          public val userInfo: Column<String> = varchar("super_important_field", 128)
        }

        public class UserEntity(
          id: EntityID<UUID>
        ) : UUIDEntity(id) {
          public var userInfo: String by UserTable.userInfo

          public companion object : UUIDEntityClass<UserEntity>(UserTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a boolean column type") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Domain("Facts")
        interface Facts {
          val isFact: Boolean
        }

        @Table
        interface FactTableSpec : Facts
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
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "FactsTable.kt" }.readTrimmed() shouldBe kotlinCode(
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
          public val isFact: Column<Boolean> = bool("is_fact")
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
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Domain("BigNum")
        interface BigNum {
          val size: Long
        }

        @Table
        interface BigNumTableSpec : BigNum
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
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "BigNumTable.kt" }.readTrimmed() shouldBe kotlinCode(
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
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Table

        @Domain("Floaty")
        interface Floaty {
          val pointyNum: Float
        }

        @Table
        interface FloatyTableSpec : Floaty
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
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "FloatyTable.kt" }.readTrimmed() shouldBe kotlinCode(
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
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.exposed.VarChar
        import io.bkbn.stoik.exposed.Table

        @Domain("Words")
        interface Words {
          val word: String
        }

        @Table
        interface WordsTableSpec : Words {
          @VarChar(size = 256)
          override val word: String
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
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "WordsTable.kt" }.readTrimmed() shouldBe kotlinCode(
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
  describe("Dao Generation") {
    it("Can create a simple dao") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.exposed.Table

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
        symbolProcessorProviders = listOf(ExposedProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.stoik.generated.table

        import io.bkbn.stoik.core.dao.Dao
        import io.bkbn.stoik.generated.models.UserCreateRequest
        import io.bkbn.stoik.generated.models.UserEntity
        import io.bkbn.stoik.generated.models.UserResponse
        import io.bkbn.stoik.generated.models.UserUpdateRequest
        import java.util.UUID
        import kotlinx.datetime.Clock
        import org.jetbrains.exposed.sql.transactions.transaction

        public class UserDao : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
          public override fun create(request: UserCreateRequest): UserResponse = transaction {
            val now = Clock.now()
            val entity = transaction {
              UserEntity.new {
                firstName = request.firstName
                lastName = request.lastName
                createdAt = now
                updatedAt = now
              }
            }
            entity.toResponse()
          }

          public override fun read(id: UUID): UserResponse = transaction {
            val entity = UserEntity.findById(id) ?: error("PLACEHOLDER")
            entity.toResponse()
          }

          public override fun update(id: UUID, request: UserUpdateRequest): UserResponse = transaction {
            val now = Clock.now()
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
        }
      """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
  }
  describe("File Creation") {
    it("Can construct multiple tables in a single source set") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.stoik.core.Domain
        import io.bkbn.stoik.exposed.VarChar
        import io.bkbn.stoik.exposed.Table

        @Domain("Words")
        interface Words {
          val word: String
        }

        @Table
        interface WordsTable : Words {
          @VarChar(size = 256)
          override val word: String
        }

        @Domain("OtherWords")
        interface OtherWords {
          val wordy: String
        }

        @Table
        interface OtherWordsTableSpec : OtherWords {
          @VarChar(size = 128)
          override val wordy: String
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
      result.kspGeneratedSources shouldHaveSize 4

      val fileNames = result.kspGeneratedSources.map { it.name }
      fileNames shouldContain "OtherWordsTable.kt"
      fileNames shouldContain "WordsTable.kt"
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
  }
}
