package io.bkbn.lerasium.rdbms.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.bkbn.lerasium.utils.TestUtils.errorMessage
import io.bkbn.lerasium.utils.TestUtils.kotlinCode
import io.bkbn.lerasium.utils.TestUtils.kspGeneratedSources
import io.bkbn.lerasium.utils.TestUtils.readTrimmed
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RdbmsProcessorProviderTest : DescribeSpec({
  describe("Table Generation") {
    it("Can construct a simple Table with a single column") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.Table

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
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.UserResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object UserTable : UUIDTable("user") {
          public val name: Column<String> = varchar("name", 128)

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class UserEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<UserResponse> {
          public var name: String by UserTable.name

          public var createdAt: LocalDateTime by UserTable.createdAt

          public var updatedAt: LocalDateTime by UserTable.updatedAt

          public override fun toResponse(): UserResponse = with(::UserResponse) {
            val propertiesByName = UserEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                UserResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@UserEntity)
              }
            }
            callBy(params)
          }

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

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.Table

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
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "CounterTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.CounterResponse
        import java.util.UUID
        import kotlin.Int
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object CounterTable : UUIDTable("counter") {
          public val count: Column<Int> = integer("count")

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class CounterEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<CounterResponse> {
          public var count: Int by CounterTable.count

          public var createdAt: LocalDateTime by CounterTable.createdAt

          public var updatedAt: LocalDateTime by CounterTable.updatedAt

          public override fun toResponse(): CounterResponse = with(::CounterResponse) {
            val propertiesByName = CounterEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                CounterResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@CounterEntity)
              }
            }
            callBy(params)
          }

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

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.Column
        import io.bkbn.lerasium.rdbms.Table

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
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.UserResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object UserTable : UUIDTable("user") {
          public val userInfo: Column<String> = varchar("super_important_field", 128)

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class UserEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<UserResponse> {
          public var userInfo: String by UserTable.userInfo

          public var createdAt: LocalDateTime by UserTable.createdAt

          public var updatedAt: LocalDateTime by UserTable.updatedAt

          public override fun toResponse(): UserResponse = with(::UserResponse) {
            val propertiesByName = UserEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                UserResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@UserEntity)
              }
            }
            callBy(params)
          }

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

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.Column
        import io.bkbn.lerasium.rdbms.Table

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
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "FactsTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.FactsResponse
        import java.util.UUID
        import kotlin.Boolean
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object FactsTable : UUIDTable("facts") {
          public val isFact: Column<Boolean> = bool("is_fact")

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class FactsEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<FactsResponse> {
          public var isFact: Boolean by FactsTable.isFact

          public var createdAt: LocalDateTime by FactsTable.createdAt

          public var updatedAt: LocalDateTime by FactsTable.updatedAt

          public override fun toResponse(): FactsResponse = with(::FactsResponse) {
            val propertiesByName = FactsEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                FactsResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@FactsEntity)
              }
            }
            callBy(params)
          }

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

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.Column
        import io.bkbn.lerasium.rdbms.Table

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
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "BigNumTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.BigNumResponse
        import java.util.UUID
        import kotlin.Long
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object BigNumTable : UUIDTable("big_num") {
          public val size: Column<Long> = long("size")

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class BigNumEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<BigNumResponse> {
          public var size: Long by BigNumTable.size

          public var createdAt: LocalDateTime by BigNumTable.createdAt

          public var updatedAt: LocalDateTime by BigNumTable.updatedAt

          public override fun toResponse(): BigNumResponse = with(::BigNumResponse) {
            val propertiesByName = BigNumEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                BigNumResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@BigNumEntity)
              }
            }
            callBy(params)
          }

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

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.Column
        import io.bkbn.lerasium.rdbms.Table

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
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "FloatyTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.FloatyResponse
        import java.util.UUID
        import kotlin.Float
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object FloatyTable : UUIDTable("floaty") {
          public val pointyNum: Column<Float> = float("pointy_num")

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class FloatyEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<FloatyResponse> {
          public var pointyNum: Float by FloatyTable.pointyNum

          public var createdAt: LocalDateTime by FloatyTable.createdAt

          public var updatedAt: LocalDateTime by FloatyTable.updatedAt

          public override fun toResponse(): FloatyResponse = with(::FloatyResponse) {
            val propertiesByName = FloatyEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                FloatyResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@FloatyEntity)
              }
            }
            callBy(params)
          }

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

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.VarChar
        import io.bkbn.lerasium.rdbms.Table

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
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "WordsTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.WordsResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object WordsTable : UUIDTable("words") {
          public val word: Column<String> = varchar("word", 256)

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class WordsEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<WordsResponse> {
          public var word: String by WordsTable.word

          public var createdAt: LocalDateTime by WordsTable.createdAt

          public var updatedAt: LocalDateTime by WordsTable.updatedAt

          public override fun toResponse(): WordsResponse = with(::WordsResponse) {
            val propertiesByName = WordsEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                WordsResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@WordsEntity)
              }
            }
            callBy(params)
          }

          public companion object : UUIDEntityClass<WordsEntity>(WordsTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with nullable fields") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.Table

        @Domain("Letters")
        interface Letters {
          val s: String?
          val i: Int?
          val l: Long?
          val b: Boolean?
          val d: Double?
          val f: Float?
        }

        @Table
        interface LetterTable : Letters
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
      result.kspGeneratedSources.first { it.name == "LettersTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.LettersResponse
        import java.util.UUID
        import kotlin.Boolean
        import kotlin.Double
        import kotlin.Float
        import kotlin.Int
        import kotlin.Long
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object LettersTable : UUIDTable("letters") {
          public val s: Column<String?> = varchar("s", 128).nullable()

          public val i: Column<Int?> = integer("i").nullable()

          public val l: Column<Long?> = long("l").nullable()

          public val b: Column<Boolean?> = bool("b").nullable()

          public val d: Column<Double?> = double("d").nullable()

          public val f: Column<Float?> = float("f").nullable()

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class LettersEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<LettersResponse> {
          public var s: String? by LettersTable.s

          public var i: Int? by LettersTable.i

          public var l: Long? by LettersTable.l

          public var b: Boolean? by LettersTable.b

          public var d: Double? by LettersTable.d

          public var f: Float? by LettersTable.f

          public var createdAt: LocalDateTime by LettersTable.createdAt

          public var updatedAt: LocalDateTime by LettersTable.updatedAt

          public override fun toResponse(): LettersResponse = with(::LettersResponse) {
            val propertiesByName = LettersEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                LettersResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@LettersEntity)
              }
            }
            callBy(params)
          }

          public companion object : UUIDEntityClass<LettersEntity>(LettersTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a indexed field") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.persistence.Index
        import io.bkbn.lerasium.rdbms.Table

        @Domain("Words")
        interface Words {
          val word: String
        }

        @Table
        interface WordsTableSpec : Words {
          @Index
          override val word: String
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
      result.kspGeneratedSources.first { it.name == "WordsTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.WordsResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object WordsTable : UUIDTable("words") {
          public val word: Column<String> = varchar("word", 128).index()

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class WordsEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<WordsResponse> {
          public var word: String by WordsTable.word

          public var createdAt: LocalDateTime by WordsTable.createdAt

          public var updatedAt: LocalDateTime by WordsTable.updatedAt

          public override fun toResponse(): WordsResponse = with(::WordsResponse) {
            val propertiesByName = WordsEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                WordsResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@WordsEntity)
              }
            }
            callBy(params)
          }

          public companion object : UUIDEntityClass<WordsEntity>(WordsTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a unique index field") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.persistence.Index
        import io.bkbn.lerasium.rdbms.Table

        @Domain("Words")
        interface Words {
          val word: String
        }

        @Table
        interface WordsTableSpec : Words {
          @Index(unique = true)
          override val word: String
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
      result.kspGeneratedSources.first { it.name == "WordsTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.WordsResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object WordsTable : UUIDTable("words") {
          public val word: Column<String> = varchar("word", 128).uniqueIndex()

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class WordsEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<WordsResponse> {
          public var word: String by WordsTable.word

          public var createdAt: LocalDateTime by WordsTable.createdAt

          public var updatedAt: LocalDateTime by WordsTable.updatedAt

          public override fun toResponse(): WordsResponse = with(::WordsResponse) {
            val propertiesByName = WordsEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                WordsResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@WordsEntity)
              }
            }
            callBy(params)
          }

          public companion object : UUIDEntityClass<WordsEntity>(WordsTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a composite index") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.persistence.CompositeIndex
        import io.bkbn.lerasium.rdbms.Table

        @Domain("Words")
        interface Words {
          val word: String
          val language: String
        }

        @Table
        @CompositeIndex(true, "word", "language")
        interface WordsTableSpec : Words {
          override val word: String
          override val language: String
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
      result.kspGeneratedSources.first { it.name == "WordsTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.WordsResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object WordsTable : UUIDTable("words") {
          public val word: Column<String> = varchar("word", 128)

          public val language: Column<String> = varchar("language", 128)

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")

          init {
            index(true, word, language)
          }
        }

        public class WordsEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<WordsResponse> {
          public var word: String by WordsTable.word

          public var language: String by WordsTable.language

          public var createdAt: LocalDateTime by WordsTable.createdAt

          public var updatedAt: LocalDateTime by WordsTable.updatedAt

          public override fun toResponse(): WordsResponse = with(::WordsResponse) {
            val propertiesByName = WordsEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                WordsResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@WordsEntity)
              }
            }
            callBy(params)
          }

          public companion object : UUIDEntityClass<WordsEntity>(WordsTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a foreign key reference") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import java.util.UUID
        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.ForeignKey
        import io.bkbn.lerasium.rdbms.Table

        @Domain("Country")
        interface Country {
          val name: String
        }

        @Table
        interface CountryTable : Country

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
      result.kspGeneratedSources.first { it.name == "UserTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.UserResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object UserTable : UUIDTable("user") {
          public val country: Column<EntityID<UUID>> = reference("country", CountryTable)

          public val name: Column<String> = varchar("name", 128)

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class UserEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<UserResponse> {
          public var country: CountryEntity by CountryEntity referencedOn UserTable.country

          public var name: String by UserTable.name

          public var createdAt: LocalDateTime by UserTable.createdAt

          public var updatedAt: LocalDateTime by UserTable.updatedAt

          public override fun toResponse(): UserResponse = with(::UserResponse) {
            val propertiesByName = UserEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                UserResponse::id.name -> id.value
                UserEntity::country.name -> country.toResponse()
                else -> propertiesByName[it.name]?.get(this@UserEntity)
              }
            }
            callBy(params)
          }

          public companion object : UUIDEntityClass<UserEntity>(UserTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a one-to-many reference") {
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
      result.kspGeneratedSources.first { it.name == "CountryTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.CountryResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.SizedIterable
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object CountryTable : UUIDTable("country") {
          public val name: Column<String> = varchar("name", 128)

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class CountryEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<CountryResponse> {
          public var name: String by CountryTable.name

          public var createdAt: LocalDateTime by CountryTable.createdAt

          public var updatedAt: LocalDateTime by CountryTable.updatedAt

          public val users: SizedIterable<UserEntity> by UserEntity referrersOn UserTable.country

          public override fun toResponse(): CountryResponse = with(::CountryResponse) {
            val propertiesByName = CountryEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                CountryResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@CountryEntity)
              }
            }
            callBy(params)
          }

          public companion object : UUIDEntityClass<CountryEntity>(CountryTable)
        }
        """.trimIndent()
      )
    }
    it("Can construct a table with a many-to-many reference") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt",
        """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.core.Relation
        import io.bkbn.lerasium.rdbms.ForeignKey
        import io.bkbn.lerasium.rdbms.ManyToMany
        import io.bkbn.lerasium.rdbms.Table

        @Domain("User")
        sealed interface User {
          val name: String
          @Relation
          val books: Book
        }

        @Table
        interface UserTable : User {
          @ManyToMany(BookReview::class)
          override val books: Book
        }

        @Domain("Book")
        sealed interface Book {
          val title: String
          @Relation
          val readers: User
        }

        @Table
        interface BookTable : Book {
          @ManyToMany(BookReview::class)
          override val readers: User
        }

        @Domain("BookReview")
        sealed interface BookReview {
          val reader: User
          val book: Book
          val rating: Int
        }

        @Table
        interface BookReviewTable : BookReview {
          @ForeignKey
          override val reader: User
          @ForeignKey
          override val book: Book
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
      result.kspGeneratedSources shouldHaveSize 6
      result.kspGeneratedSources.first { it.name == "BookTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.BookResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.SizedIterable
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object BookTable : UUIDTable("book") {
          public val title: Column<String> = varchar("title", 128)

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class BookEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<BookResponse> {
          public var title: String by BookTable.title

          public var createdAt: LocalDateTime by BookTable.createdAt

          public var updatedAt: LocalDateTime by BookTable.updatedAt

          public val readers: SizedIterable<UserEntity> by UserEntity via BookReviewTable

          public override fun toResponse(): BookResponse = with(::BookResponse) {
            val propertiesByName = BookEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                BookResponse::id.name -> id.value
                else -> propertiesByName[it.name]?.get(this@BookEntity)
              }
            }
            callBy(params)
          }

          public companion object : UUIDEntityClass<BookEntity>(BookTable)
        }
        """.trimIndent()
      )
      result.kspGeneratedSources.first { it.name == "BookReviewTable.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.BookReviewResponse
        import java.util.UUID
        import kotlin.Int
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import org.jetbrains.exposed.dao.UUIDEntity
        import org.jetbrains.exposed.dao.UUIDEntityClass
        import org.jetbrains.exposed.dao.id.EntityID
        import org.jetbrains.exposed.dao.id.UUIDTable
        import org.jetbrains.exposed.sql.Column
        import org.jetbrains.exposed.sql.kotlin.datetime.datetime

        public object BookReviewTable : UUIDTable("book_review") {
          public val reader: Column<EntityID<UUID>> = reference("reader", UserTable)

          public val book: Column<EntityID<UUID>> = reference("book", BookTable)

          public val rating: Column<Int> = integer("rating")

          public val createdAt: Column<LocalDateTime> = datetime("created_at")

          public val updatedAt: Column<LocalDateTime> = datetime("updated_at")
        }

        public class BookReviewEntity(
          id: EntityID<UUID>,
        ) : UUIDEntity(id), Entity<BookReviewResponse> {
          public var reader: UserEntity by UserEntity referencedOn BookReviewTable.reader

          public var book: BookEntity by BookEntity referencedOn BookReviewTable.book

          public var rating: Int by BookReviewTable.rating

          public var createdAt: LocalDateTime by BookReviewTable.createdAt

          public var updatedAt: LocalDateTime by BookReviewTable.updatedAt

          public override fun toResponse(): BookReviewResponse = with(::BookReviewResponse) {
            val propertiesByName = BookReviewEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                BookReviewResponse::id.name -> id.value
                BookReviewEntity::reader.name -> reader.toResponse()
                BookReviewEntity::book.name -> book.toResponse()
                else -> propertiesByName[it.name]?.get(this@BookReviewEntity)
              }
            }
            callBy(params)
          }

          public companion object : UUIDEntityClass<BookReviewEntity>(BookReviewTable)
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
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import java.util.UUID
        import kotlin.Int
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.jetbrains.exposed.sql.transactions.transaction

        public class UserDao : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
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
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.CountryCreateRequest
        import io.bkbn.lerasium.generated.models.CountryResponse
        import io.bkbn.lerasium.generated.models.CountryUpdateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import java.util.UUID
        import kotlin.Int
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.jetbrains.exposed.sql.transactions.transaction

        public class CountryDao :
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
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import java.util.UUID
        import kotlin.Int
        import kotlin.String
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.jetbrains.exposed.sql.transactions.transaction

        public class UserDao : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
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
  describe("File Creation") {
    it("Can construct multiple tables in a single source set") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.rdbms.VarChar
        import io.bkbn.lerasium.rdbms.Table

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
        symbolProcessorProviders = listOf(RdbmsProcessorProvider())
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
})
