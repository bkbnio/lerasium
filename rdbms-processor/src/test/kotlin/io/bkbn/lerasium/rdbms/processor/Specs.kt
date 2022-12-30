package io.bkbn.lerasium.rdbms.processor

import com.tschuchort.compiletesting.SourceFile

object Specs {

  val domainWithStringColumn = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithIntColumn = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWitColumnNameOverride = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithBooleanColumn = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithLongColumn = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithFloatColumn = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithCustomVarcharSize = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithNullableFields = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithIndexedField = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithUniqueIndexedField = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithCompositeIndexedField = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithForeignKeyReference = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithOneToManyReference = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithManyToManyReference = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val multipleDomains = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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

  val domainWithMultipleIndices = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
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
}
