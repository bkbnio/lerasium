package io.bkbn.lerasium.api.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude

class ControllerVisitorTest : DescribeSpec({
  describe("Validation") {
    it("Throws error when domain is not provided") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.lerasium.api.Api

          @Api
          interface User
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
      result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
      result.messages shouldInclude "User is not annotated with a valid domain!"
    }
    it("Throws error in event of invalid domain") {
      // arrange
      val sourceFile = SourceFile.kotlin(
        "Spec.kt", """
          package test

          import io.bkbn.lerasium.core.Domain
          import io.bkbn.lerasium.api.Api

          @Api
          @Domain("user")
          interface UserDomain
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
      result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
      result.messages shouldInclude "Domain is invalid"
    }
  }
  describe("Controller Generation") {
    it("Can build an API with simple CRUD functionality") {
      verifyGeneratedCode(
        source = "spec/001__spec_with_single_field.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserController.kt",
        fileSnapshot = "snapshot/T003__controller_simple_crud.txt",
      )
    }
    xit("Can build a route to access a relational member") {
      verifyGeneratedCode(
        source = "spec/002__spec_with_relational_member.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 9,
        fileUnderTest = "CountryController.kt",
        fileSnapshot = "snapshot/T004__controller_with_relational_member.txt",
      )
    }
    xit("Can build routes for getBy queries") {
      verifyGeneratedCode(
        source = "spec/003__spec_with_get_by_query.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserController.kt",
        fileSnapshot = "snapshot/T005__controller_with_get_by_query.txt",
      )
    }
    it("Can build routes for actor authentication") {
      verifyGeneratedCode(
        source = "spec/004__spec_with_actor_auth.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserController.kt",
        fileSnapshot = "snapshot/T006__controller_with_actor_auth.txt",
      )
    }
  }
})
