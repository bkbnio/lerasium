package io.bkbn.lerasium.core.processor

import io.bkbn.lerasium.core.processor.Specs.domainWithDeeplyNestedModel
import io.bkbn.lerasium.core.processor.Specs.domainWithNestedModel
import io.bkbn.lerasium.core.processor.Specs.domainWithSensitiveField
import io.bkbn.lerasium.core.processor.Specs.domainWithSimpleReference
import io.bkbn.lerasium.core.processor.Specs.domainWithUuidField
import io.bkbn.lerasium.core.processor.Specs.simpleDomain
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class ModelProcessorProviderTest : DescribeSpec({
  describe("Validation") {
    // todo
  }
  describe("Basic Model Tests") {
    it("Can generate a file with create, update and response models") {
      verifyGeneratedCode(
        source = simpleDomain,
        provider = DomainProcessorProvider(),
        expectedFileCount = 1,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T001__models_basic.txt"
      )
    }
    it("Can generate models with nested domain models") {
      verifyGeneratedCode(
        source = domainWithNestedModel,
        provider = DomainProcessorProvider(),
        expectedFileCount = 1,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T002__models_nested.txt"
      )
    }
    it("Can support a domain with deeply nested models") {
      verifyGeneratedCode(
        source = domainWithDeeplyNestedModel,
        provider = DomainProcessorProvider(),
        expectedFileCount = 1,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T003__models_deeply_nested.txt"
      )
    }
    it("Is aware of fields marked as sensitive") {
      verifyGeneratedCode(
        source = domainWithSensitiveField,
        provider = DomainProcessorProvider(),
        expectedFileCount = 1,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T004__models_sensitive_field.txt"
      )
    }
    it("Applies a UUID serializer to any UUID type") {
      verifyGeneratedCode(
        source = domainWithUuidField,
        provider = DomainProcessorProvider(),
        expectedFileCount = 1,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T005__models_uuid_serializer.txt"
      )
    }
    it("Can handle a simple reference to another domain") {
      verifyGeneratedCode(
        source = domainWithSimpleReference,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T006__models_simple_reference.txt"
      )
    }
  }
})
