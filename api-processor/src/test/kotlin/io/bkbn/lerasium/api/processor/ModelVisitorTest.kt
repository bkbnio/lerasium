package io.bkbn.lerasium.api.processor

import io.bkbn.lerasium.api.processor.Specs.deeplyNestedSpec
import io.bkbn.lerasium.api.processor.Specs.minimalSpec
import io.bkbn.lerasium.api.processor.Specs.nestedSpec
import io.bkbn.lerasium.api.processor.Specs.specWithSensitiveValue
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class ModelVisitorTest : DescribeSpec({
  describe("Model Generation Test") {
    it("Can generate models with basic CRUD functionality") {
      verifyGeneratedCode(
        source = minimalSpec,
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T013__model_basic_example.txt",
      )
    }
    it("Can generate models without leaking sensitive values") {
      verifyGeneratedCode(
        source = specWithSensitiveValue,
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T014__model_with_sensitive_values.txt",
      )
    }
    it("Can generate a nested model") {
      verifyGeneratedCode(
        source = nestedSpec,
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T015__model_with_nested.txt",
      )
    }
    it("Can generate a deeply nested model") {
      verifyGeneratedCode(
        source = deeplyNestedSpec,
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "T016__model_with_deeply_nested.txt",
      )
    }
  }
})
