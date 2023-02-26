package io.bkbn.lerasium.api.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class ModelVisitorTest : DescribeSpec({
  describe("Model Generation Test") {
    it("Can generate models with basic CRUD functionality") {
      verifyGeneratedCode(
        source = "spec/001__spec_with_single_field.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "snapshot/T013__model_basic_example.txt",
      )
    }
    it("Can generate models without leaking sensitive values") {
      verifyGeneratedCode(
        source = "spec/007__spec_with_sensitive_field.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "snapshot/T014__model_with_sensitive_values.txt",
      )
    }
    it("Can generate a nested model") {
      verifyGeneratedCode(
        source = "spec/005__spect_with_nested_model.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "snapshot/T015__model_with_nested.txt",
      )
    }
    it("Can generate a deeply nested model") {
      verifyGeneratedCode(
        source = "spec/006__spec_with_deeply_nested_model.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserModels.kt",
        fileSnapshot = "snapshot/T016__model_with_deeply_nested.txt",
      )
    }
  }
})
