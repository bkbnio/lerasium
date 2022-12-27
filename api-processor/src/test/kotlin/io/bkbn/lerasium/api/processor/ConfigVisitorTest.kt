package io.bkbn.lerasium.api.processor

import io.bkbn.lerasium.api.processor.Specs.minimalSpec
import io.bkbn.lerasium.api.processor.Specs.simpleSpecWithActor
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class ConfigVisitorTest : DescribeSpec({
  describe("Config File Tests") {
    it("Can generate a simple config") {
      verifyGeneratedCode(
        source = minimalSpec,
        provider = KtorProcessorProvider(),
        expectedFileCount = 4,
        fileUnderTest = "ApiConfig.kt",
        fileSnapshot = "T001__config_basic_example.txt",
      )
    }
    it("Can generate a config with jwt auth") {
      verifyGeneratedCode(
        source = simpleSpecWithActor,
        provider = KtorProcessorProvider(),
        expectedFileCount = 4,
        fileUnderTest = "ApiConfig.kt",
        fileSnapshot = "T002__config_with_actor.txt",
      )
    }
  }
})
