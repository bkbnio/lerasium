package io.bkbn.lerasium.core.processor

import io.bkbn.lerasium.core.processor.Specs.domainWithOneToManyReference
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class DomainVisitorTest : DescribeSpec({
  describe("Basic Domain Tests") {
    it("Can build a domain with a one-to-many relationship") {
      verifyGeneratedCode(
        source = domainWithOneToManyReference,
        provider = DomainProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "Country.kt",
        fileSnapshot = "T006__domain_one_to_many.txt"
      )
    }
  }
})
