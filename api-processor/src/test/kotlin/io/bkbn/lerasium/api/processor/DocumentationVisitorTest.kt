package io.bkbn.lerasium.api.processor

import io.bkbn.lerasium.api.processor.Specs.minimalSpec
import io.bkbn.lerasium.api.processor.Specs.simpleSpecWithActor
import io.bkbn.lerasium.api.processor.Specs.simpleSpecWithQuery
import io.bkbn.lerasium.api.processor.Specs.simpleSpecWithRelation
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class DocumentationVisitorTest : DescribeSpec({
  describe("Documentation Generation") {
    it("Can generate documentation for a simple route") {
      verifyGeneratedCode(
        source = minimalSpec,
        provider = KtorProcessorProvider(),
        expectedFileCount = 4,
        fileUnderTest = "UserDocumentation.kt",
        fileSnapshot = "T007__documentation_basic_example.txt",
      )
    }
    it("Can document access to a relational member") {
      verifyGeneratedCode(
        source = simpleSpecWithRelation,
        provider = KtorProcessorProvider(),
        expectedFileCount = 7,
        fileUnderTest = "CountryDocumentation.kt",
        fileSnapshot = "T008__documentation_with_relation.txt",
      )
    }
    it("Can build documentation for queries") {
      verifyGeneratedCode(
        source = simpleSpecWithQuery,
        provider = KtorProcessorProvider(),
        expectedFileCount = 4,
        fileUnderTest = "UserDocumentation.kt",
        fileSnapshot = "T009__documentation_with_query.txt",
      )
    }
    it("Can build documentation for actor authentication") {
      verifyGeneratedCode(
        source = simpleSpecWithActor,
        provider = KtorProcessorProvider(),
        expectedFileCount = 4,
        fileUnderTest = "UserDocumentation.kt",
        fileSnapshot = "T010__documentation_with_actor.txt",
      )
    }
  }
})
