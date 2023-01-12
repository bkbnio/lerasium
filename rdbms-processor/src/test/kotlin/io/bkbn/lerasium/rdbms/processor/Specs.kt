package io.bkbn.lerasium.rdbms.processor

import com.tschuchort.compiletesting.SourceFile

object Specs {

  val domainWithBasicTypes = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.rdbms.Table

      @Table
      @Domain("User")
      interface User {
        val name: String
        val count: Int
        val isFact: Boolean
        val size: Long
        val pointyNum: Float
      }
    """.trimIndent()
  )

  val domainWitColumnNameOverride = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.rdbms.Column
      import io.bkbn.lerasium.rdbms.Table

      @Table
      @Domain("User")
      interface User {
        @Column("super_important_field")
        val userInfo: String
      }
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
      @Table
      interface Words {
        @VarChar(256)
        val word: String
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
      @Table
      interface Letters {
        val s: String?
        val i: Int?
        val l: Long?
        val b: Boolean?
        val d: Double?
        val f: Float?
      }
    """.trimIndent()
  )

  val domainWithIndexedField = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.persistence.Index
      import io.bkbn.lerasium.rdbms.Table

      @Table
      @Domain("Words")
      interface Words {
        @Index
        val word: String
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
      @Table
      interface Words {
        @Index(unique = true)
        val word: String
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
      @Table
      @CompositeIndex(true, "word", "language")
      interface Words {
        val word: String
        val language: String
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
      @Table
      interface Country {
        val name: String
      }

      @Domain("User")
      @Table
      interface User {
        val name: String
        @ForeignKey
        val country: Country
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
      @Table
      interface Country {
        val name: String
        @Relation
        @OneToMany("country")
        val users: List<User>
      }

      @Domain("User")
      @Table
      interface User {
        val name: String
        @ForeignKey
        val country: Country
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
      @Table
      interface User {
        val name: String
        @Relation
        @ManyToMany(BookReview::class)
        val books: List<Book>
      }

      @Domain("Book")
      @Table
      interface Book {
        val title: String
        @Relation
        @ManyToMany(BookReview::class)
        val readers: List<User>
      }

      @Domain("BookReview")
      @Table
      interface BookReview {
        @ForeignKey
        val reader: User
        @ForeignKey
        val book: Book
        val rating: Int
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
      @Table
      interface Words {
        @VarChar(256)
        val word: String
      }

      @Domain("OtherWords")
      @Table
      interface OtherWords {
        @VarChar(128)
        val wordy: String
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
      @Table
      interface User {
        @Index(true)
        val email: String
        @Index
        val favoriteFood: String
      }
    """.trimIndent()
  )

  val domainWithSensitiveField = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.persistence.Sensitive
      import io.bkbn.lerasium.rdbms.Table

      @Domain("User")
      @Table
      interface User {
        val email: String
        @Sensitive
        val password: String
      }
    """.trimIndent()
  )

  val domainWithRbacPolicy = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.rdbms.Table
      import io.bkbn.lerasium.rdbms.ForeignKey
      import io.bkbn.lerasium.core.auth.CrudAction
      import io.bkbn.bouncer.core.rbacPolicy
      import io.bkbn.lerasium.core.auth.RbacPolicyProvider

      @Domain("User")
      @Table
      @Policy("user")
      interface User {
        val email: String
        val password: String
      }

      @Api
      @Domain("OrganizationRole")
      @Table
      interface OrganizationRole {
        @ForeignKey
        val organization: Organization
        @ForeignKey
        val user: User
        val role: Type

        @Serializable
        enum class Type {
          ADMIN,
          MAINTAINER,
          CONTRIBUTOR
        }
      }

      @Api
      @Domain("Organization")
      @Table
      interface Organization {
        val name: String

        companion object {
          val userRbac =
            object : RbacPolicyProvider<User, CrudAction, OrganizationRole, OrganizationRole.Type, Organization> {
              override val policy = rbacPolicy<User, CrudAction, OrganizationRole.Type, Organization> {
                can(
                  "Admin can always delete an organization",
                  CrudAction.DELETE,
                  OrganizationRole.Type.ADMIN
                ) { _, _, _ -> true }
              }
            }
        }
      }
    """.trimIndent()
  )
}
