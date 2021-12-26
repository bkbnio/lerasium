package io.bkbn.stoik.exposed.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe

class ExposedProcessorProviderTest : DescribeSpec({
  describe("Testing Testing 123") {
    it("Can do the things") {
      // arrange
      val sourceFile = SourceFile.kotlin("Demo.kt", """
        import io.bkbn.stoik.exposed.Column
        import io.bkbn.stoik.exposed.Sensitive
        import io.bkbn.stoik.exposed.Table
        import io.bkbn.stoik.exposed.Unique

        sealed interface UserSpec {
          val firstName: String
          val lastName: String
          val email: String
          val password: String
        }

        @Table("user")
        interface UserTableSpec : UserSpec {
          @Column("first_name")
          override val firstName: String

          @Column("last_name")
          override val lastName: String

          @Unique
          override val email: String

          @Sensitive
          override val password: String
        }
      """.trimIndent())
      val compilation = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(ExposedProcessorProvider())
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
//      result.generatedFiles shouldHaveSize 1
    }
  }
})
