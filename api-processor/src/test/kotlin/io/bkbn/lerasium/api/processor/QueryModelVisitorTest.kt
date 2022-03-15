package io.bkbn.lerasium.api.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.bkbn.lerasium.utils.TestUtils
import io.bkbn.lerasium.utils.TestUtils.kspGeneratedSources
import io.bkbn.lerasium.utils.TestUtils.readTrimmed
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class QueryModelVisitorTest : DescribeSpec({
  describe("Get By Queries") {
    it("Can construct query models for getBy requests") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
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

      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(KtorProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 3
      result.kspGeneratedSources.first { it.name == "UserQueries.kt" }.readTrimmed() shouldBe TestUtils.kotlinCode(
        """
        package io.bkbn.lerasium.generated.api

        import io.bkbn.kompendium.annotations.Param
        import io.bkbn.kompendium.annotations.ParamType
        import kotlin.Int
        import kotlin.String

        public data class GetUserByEmailQuery(
          @Param(ParamType.PATH)
          public val email: String
        )

        public data class GetUserByFirstNameQuery(
          @Param(ParamType.PATH)
          public val firstName: String,
          @Param(ParamType.QUERY)
          public val chunk: Int = 100,
          @Param(ParamType.QUERY)
          public val offset: Int = 0
        )
        """.trimIndent()
      )
    }
  }
})
