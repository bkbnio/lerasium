package io.bkbn.lerasium.core.processor

import com.tschuchort.compiletesting.SourceFile

object Specs {

  val simpleDomain = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain

      @Domain("User")
      interface UserDomain {
        val firstName: String
        val lastName: String
        val email: String
        val password: String
        val age: Int?
      }
    """.trimIndent()
  )

  val domainWithNestedModel = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain

      @Domain("User")
      interface UserDomain {
        val email: String
        val metadata: UserMetadata
      }

      interface UserMetadata {
        val firstName: String
        val lastName: String
      }
    """.trimIndent()
  )

  val domainWithDeeplyNestedModel = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain

      @Domain("User")
      interface UserDomain {
        val email: String
        val metadata: UserMetadata
      }

      interface UserMetadata {
        val firstName: String
        val lastName: String
        val otherStuffs: OhBoiWeDeepInItNow
      }

      interface OhBoiWeDeepInItNow {
        val otherInfo: String
      }
    """.trimIndent()
  )

  val domainWithSensitiveField = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.core.Sensitive

      @Domain("User")
      interface UserDomain {
        val firstName: String
        val lastName: String
        val email: String
        @Sensitive
        val password: String
      }
    """.trimIndent()
  )

  val domainWithUuidField = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.core.Sensitive
      import java.util.UUID

      @Domain("User")
      interface UserDomain {
        val firstName: String
        val lastName: String
        val email: String
        val favoriteUuid: UUID,
      }
    """.trimIndent()
  )

  val domainWithSimpleReference = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.core.Sensitive
      import java.util.UUID

      @Domain("Country")
      interface Country {
        val name: String
      }

      @Domain("User")
      interface UserDomain {
        val firstName: String
        val lastName: String
        val email: String
        val country: Country
      }
    """.trimIndent()
  )
}
