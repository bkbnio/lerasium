package io.bkbn.lerasium.core.processor

import io.bkbn.lerasium.core.processor.Specs.domainWithDeeplyNestedModel
import io.bkbn.lerasium.core.processor.Specs.domainWithEnum
import io.bkbn.lerasium.core.processor.Specs.domainWithNestedModel
import io.bkbn.lerasium.core.processor.Specs.domainWithOneToManyReference
import io.bkbn.lerasium.core.processor.Specs.domainWithSensitiveField
import io.bkbn.lerasium.core.processor.Specs.domainWithSimpleReference
import io.bkbn.lerasium.core.processor.Specs.domainWithUuidField
import io.bkbn.lerasium.core.processor.Specs.simpleDomain
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class DomainVisitorTest : DescribeSpec({
  describe("Basic Domain Tests") {
    it("Can build a simple domain") {
      verifyGeneratedCode(
        source = simpleDomain,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "T001__domain_simple.txt"
      )
    }
    it("Can build a domain with a nested model") {
      verifyGeneratedCode(
        source = domainWithNestedModel,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "T002__domain_nested.txt"
      )
    }
    it("Can build a domain with a deeply nested model") {
      verifyGeneratedCode(
        source = domainWithDeeplyNestedModel,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "T003__domain_deeply_nested.txt"
      )
    }
    it("Can build a domain with a sensitive field") {
      verifyGeneratedCode(
        source = domainWithSensitiveField,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "T004__domain_sensitive_field.txt"
      )
    }
    it("Can build a domain with a UUID field") {
      verifyGeneratedCode(
        source = domainWithUuidField,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "T005__domain_uuid_field.txt"
      )
    }
    it("Can build a domain with a one-to-many relationship") {
      verifyGeneratedCode(
        source = domainWithOneToManyReference,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "CountryDomain.kt",
        fileSnapshot = "T006__domain_one_to_many.txt"
      )
    }
    it("Can build a domain with a simple reference") {
      verifyGeneratedCode(
        source = domainWithSimpleReference,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "T007__domain_simple_reference.txt"
      )
    }
    it("Can build a domain with an enum type") {
      verifyGeneratedCode(
        source = domainWithEnum,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDomain.kt",
        fileSnapshot = "T008__domain_enum.txt"
      )
    }
  }
})
