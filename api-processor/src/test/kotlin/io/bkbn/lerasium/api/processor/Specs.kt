package io.bkbn.lerasium.api.processor

import com.tschuchort.compiletesting.SourceFile

object Specs {

  val minimalSpec = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import io.bkbn.lerasium.core.Domain

      @Api
      @Domain("User")
      interface User {
        val name: String
      }
    """.trimIndent()
  )

  val simpleSpecWithRelation = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import java.util.UUID
      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.core.Relation

      @Api
      @Domain("Country")
      interface Country {
        val name: String
        @Relation
        val users: User
      }

      @Api
      @Domain("User")
      interface User {
        val name: String
        val country: Country
      }
    """.trimIndent()
  )

  val simpleSpecWithQuery = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import io.bkbn.lerasium.api.GetBy
      import io.bkbn.lerasium.core.Domain

      @Api
      @Domain("User")
      interface UserDomain {
        @GetBy(true)
        val email: String
        @GetBy
        val firstName: String
      }
    """.trimIndent()
  )

  val simpleSpecWithActor = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import io.bkbn.lerasium.core.auth.Username
      import io.bkbn.lerasium.core.auth.Password
      import io.bkbn.lerasium.core.auth.Actor
      import io.bkbn.lerasium.core.Domain

      @Api
      @Actor
      @Domain("User")
      interface User {
        @Username
        val username: String
        @Password
        val password: String
      }
    """.trimIndent()
  )

  val nestedSpec = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import io.bkbn.lerasium.core.Domain

      @Api
      @Domain("User")
      interface User {
        val name: String
        val address: Address

        interface Address {
          val street: String
          val city: String
        }
      }
    """.trimIndent()
  )

  val deeplyNestedSpec = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import io.bkbn.lerasium.core.Domain

      @Api
      @Domain("User")
      interface User {
        val name: String
        val address: Address

        interface Address {
          val street: String
          val city: String
          val state: State

          interface State {
            val name: String
            val country: Country

            interface Country {
              val name: String
            }
          }
        }
      }
    """.trimIndent()
  )

  val specWithSensitiveValue = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import io.bkbn.lerasium.core.Domain
      import io.bkbn.lerasium.core.Sensitive

      @Api
      @Domain("User")
      interface User {
        val name: String
        @Sensitive
        val password: String
      }
    """.trimIndent()
  )

}
