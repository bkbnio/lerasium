package io.bkbn.lerasium.mongo.processor

import com.tschuchort.compiletesting.SourceFile

object Specs {

  val domainWithDocument = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.mongo.Document

      @Document
      @Domain("User")
      interface User {
        val name: String
      }
    """.trimIndent()
  )

  val domainWithNestedDocument = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.mongo.Document

      @Document
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
    """.trimIndent()
  )

  val domainWithDeeplyNestedDocument = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.mongo.Document

      @Document
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
    """.trimIndent()
  )

  val domainWithUniqueIndex = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.mongo.Document
      import io.bkbn.lerasium.persistence.Index

      @Document
      @Domain("User")
      interface User {
        @Index(unique = true)
        val name: String
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

      @Document
      @CompositeIndex(fields = ["name", "favoriteFood"])
      @Domain("User")
      interface User {
        val name: String
        val favoriteFood: String
      }
    """.trimIndent()
  )

}
