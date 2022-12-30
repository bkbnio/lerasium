package io.bkbn.lerasium.rdbms.processor

import io.bkbn.lerasium.rdbms.processor.Specs.domainWithMultipleIndices
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithOneToManyReference
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithStringColumn
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class DaoVisitorTest : DescribeSpec({
  describe("Dao Generation") {
    it("Can construct a simple dao") {
      verifyGeneratedCode(
        source = domainWithStringColumn,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDao.kt",
        fileSnapshot = "T016__dao_simple.txt"
      )
    }
    it("Can create a dao with a one-to-many reference") {
      verifyGeneratedCode(
        source = domainWithOneToManyReference,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 4,
        fileUnderTest = "CountryDao.kt",
        fileSnapshot = "T017__dao_with_one_to_many.txt"
      )
    }
    it("Can build the appropriate index accessors") {
      verifyGeneratedCode(
        source = domainWithMultipleIndices,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDao.kt",
        fileSnapshot = "T018__dao_with_index_accessors.txt"
      )
    }
  }
})
