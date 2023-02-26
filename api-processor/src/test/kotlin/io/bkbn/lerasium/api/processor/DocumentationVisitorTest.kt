package io.bkbn.lerasium.api.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class DocumentationVisitorTest : DescribeSpec({
  describe("Documentation Generation") {
    it("Can generate documentation for a simple route") {
      verifyGeneratedCode(
        source = "spec/001__spec_with_single_field.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserDocumentation.kt",
        fileSnapshot = "snapshot/T007__documentation_basic_example.txt",
      )
    }
    it("Can document access to a relational member") {
      verifyGeneratedCode(
        source = "spec/002__spec_with_relational_member.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 9,
        fileUnderTest = "CountryDocumentation.kt",
        fileSnapshot = "snapshot/T008__documentation_with_relation.txt",
      )
    }
    it("Can build documentation for queries") {
      verifyGeneratedCode(
        source = "spec/003__spec_with_get_by_query.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserDocumentation.kt",
        fileSnapshot = "snapshot/T009__documentation_with_query.txt",
      )
    }
    it("Can build documentation for actor authentication") {
      verifyGeneratedCode(
        source = "spec/004__spec_with_actor_auth.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserDocumentation.kt",
        fileSnapshot = "snapshot/T010__documentation_with_actor.txt",
      )
    }
  }
})
