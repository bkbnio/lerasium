package io.bkbn.lerasium.mongo.processor

import io.bkbn.lerasium.mongo.processor.Specs.domainWithCompositeIndex
import io.bkbn.lerasium.mongo.processor.Specs.domainWithDeeplyNestedDocument
import io.bkbn.lerasium.mongo.processor.Specs.domainWithDocument
import io.bkbn.lerasium.mongo.processor.Specs.domainWithNestedDocument
import io.bkbn.lerasium.mongo.processor.Specs.domainWithUniqueIndex
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class DaoVisitorTest : DescribeSpec({
  describe("KMongo DAO Visitor Tests") {
    it("Can generate a simple DAO") {
      verifyGeneratedCode(
        source = domainWithDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDao.kt",
        fileSnapshot = "T004__dao_simple.txt",
      )
    }
    it("Can generate a simple nested DAO") {
      verifyGeneratedCode(
        source = domainWithNestedDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDao.kt",
        fileSnapshot = "T005__dao_nested.txt",
      )
    }
    it("Can generate a deeply nested DAO") {
      verifyGeneratedCode(
        source = domainWithDeeplyNestedDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDao.kt",
        fileSnapshot = "T006__dao_deeply_nested.txt",
      )
    }
    it("Can generate a DAO with a unique index") {
      verifyGeneratedCode(
        source = domainWithUniqueIndex,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDao.kt",
        fileSnapshot = "T007__dao_unique_index.txt",
      )
    }
    it("Can generate a DAO with a composite index") {
      verifyGeneratedCode(
        source = domainWithCompositeIndex,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDao.kt",
        fileSnapshot = "T008__dao_composite_index.txt",
      )
    }
  }
})
