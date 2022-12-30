package io.bkbn.lerasium.mongo.processor

import com.tschuchort.compiletesting.SourceFile

object Specs {

  val domainWithDocument = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.mongo.Document

      @Domain("User")
      interface User {
        val name: String
      }

      @Document
      interface UserDoc : User
    """.trimIndent()
  )

  val domainWithNestedDocument = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.mongo.Document

      @Domain("User")
      interface User {
        val name: String
        val age: Int
        val preferences: UserPreferences
      }

      interface UserPreferences {
        val status: String
        val subscribed: Boolean
      }

      @Document
      interface UserDoc : User
    """.trimIndent()
  )

  val domainWithDeeplyNestedDocument = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.mongo.Document

      @Domain("User")
      interface User {
        val preferences: UserPreferences
      }

      interface UserPreferences {
        val stuff: UserStuff
      }

      interface UserStuff {
        val info: UserInfo
      }


      interface UserInfo {
        val isCool: Boolean
      }

      @Document
      interface UserDoc : User
    """.trimIndent()
  )

  val domainWithUniqueIndex = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.mongo.Document
      import io.bkbn.lerasium.persistence.Index

      @Domain("User")
      interface User {
        val name: String
      }

      @Document
      interface UserDoc : User {
        @Index(unique = true)
        override val name: String
      }
    """.trimIndent()
  )

  val domainWithCompositeIndex = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.mongo.Document
      import io.bkbn.lerasium.persistence.CompositeIndex

      @Domain("User")
      interface User {
        val name: String
        val favoriteFood: String
      }

      @Document
      @CompositeIndex(fields = ["name", "favoriteFood"])
      interface UserDoc : User
    """.trimIndent()
  )

}
