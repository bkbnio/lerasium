package io.bkbn.lerasium.core.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class DomainVisitorTest : DescribeSpec({
  describe("Basic Domain Tests") {
    it("Can build a simple domain") {
      verifyGeneratedCode(
        source = "spec/001__domain_with_simple_model.txt",
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "snapshot/001__domain_simple.txt"
      )
    }
    it("Can build a domain with a nested model") {
      verifyGeneratedCode(
        source = "spec/002__domain_with_nested_model.txt",
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "snapshot/002__domain_nested.txt"
      )
    }
    it("Can build a domain with a deeply nested model") {
      verifyGeneratedCode(
        source = "spec/003__domain_with_deeply_nested_model.txt",
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "snapshot/003__domain_deeply_nested.txt"
      )
    }
    it("Can build a domain with a sensitive field") {
      verifyGeneratedCode(
        source = "spec/004__domain_with_sensitive_field.txt",
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "snapshot/004__domain_sensitive_field.txt"
      )
    }
    it("Can build a domain with a UUID field") {
      verifyGeneratedCode(
        source = "spec/005__domain_with_uuid_field.txt",
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "snapshot/005__domain_uuid_field.txt"
      )
    }
    xit("Can build a domain with a one-to-many relationship") {
      verifyGeneratedCode(
        source = "spec/006__domain_with_one_to_many_reference.txt",
        provider = DomainProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "CountryDomain.kt",
        fileSnapshot = "snapshot/006__domain_one_to_many.txt"
      )
    }
    it("Can build a domain with a simple reference") {
      verifyGeneratedCode(
        source = "spec/007__domain_with_simple_reference.txt",
        provider = DomainProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "snapshot/007__domain_simple_reference.txt"
      )
    }
    it("Can build a domain with an enum type") {
      verifyGeneratedCode(
        source = "spec/008__domain_with_enum.txt",
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "snapshot/008__domain_enum.txt"
      )
    }
  }
})
