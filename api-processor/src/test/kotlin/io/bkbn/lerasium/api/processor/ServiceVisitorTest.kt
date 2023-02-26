package io.bkbn.lerasium.api.processor

import io.bkbn.lerasium.api.processor.Specs.simpleSpecWithActor
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class ServiceVisitorTest : DescribeSpec({
  describe("Service Generation Test") {
    it("Can generate a service with basic CRUD functionality") {
      verifyGeneratedCode(
        source = "spec/001__spec_with_single_field.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserService.kt",
        fileSnapshot = "snapshot/T012__service_basic_example.txt",
      )
    }
    it("Can generate a service with an authentication block") {
      verifyGeneratedCode(
        source = "spec/004__spec_with_actor_auth.txt",
        provider = KtorProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserService.kt",
        fileSnapshot = "snapshot/T011__service_with_authentication.txt",
      )
    }
  }
})
