package io.bkbn.lerasium.api.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class ApiConfigVisitorTest : DescribeSpec({
  describe("Config File Tests") {
    it("Can generate a simple config") {
      verifyGeneratedCode(
        source = "spec/001__spec_with_single_field.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "ApiConfig.kt",
        fileSnapshot = "snapshot/T001__config_basic_example.txt",
      )
    }
    it("Can generate a config with jwt auth") {
      verifyGeneratedCode(
        source = "spec/004__spec_with_actor_auth.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "ApiConfig.kt",
        fileSnapshot = "snapshot/T002__config_with_actor.txt",
      )
    }
  }
})
