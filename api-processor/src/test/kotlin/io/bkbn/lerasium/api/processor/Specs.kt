package io.bkbn.lerasium.api.processor

import com.tschuchort.compiletesting.SourceFile

object Specs {

  val minimalSpec = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import io.bkbn.lerasium.core.Domain

      @Domain("User")
      interface UserDomain

      @Api("User")
      interface UserApiSpec : UserDomain
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

      @Domain("Country")
      interface Country {
        val name: String
        @Relation
        val users: User
      }

      @Api
      interface CountryApi : Country

      @Domain("User")
      interface User {
        val name: String
        val country: Country
      }

      @Api
      interface UserApi : User
    """.trimIndent()
  )

  val simpleSpecWithQuery = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import io.bkbn.lerasium.api.GetBy
      import io.bkbn.lerasium.core.Domain

      @Domain("User")
      interface UserDomain {
        val email: String
        val firstName: String
      }

      @Api("User")
      interface UserApiSpec : UserDomain {
        @GetBy(true)
        override val email: String

        @GetBy
        override val firstName: String
      }
    """.trimIndent()
  )

  val simpleSpecWithActor = SourceFile.kotlin(
    name = "Spec.kt",
    contents = """
      package test

      import io.bkbn.lerasium.api.Api
      import io.bkbn.lerasium.core.auth.Actor
      import io.bkbn.lerasium.core.Domain

      @Actor
      @Domain("User")
      interface UserDomain

      @Api("User")
      interface UserApiSpec : UserDomain
    """.trimIndent()
  )

}
