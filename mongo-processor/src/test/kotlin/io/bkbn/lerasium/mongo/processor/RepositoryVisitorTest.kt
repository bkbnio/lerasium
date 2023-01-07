package io.bkbn.lerasium.mongo.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class RepositoryVisitorTest : DescribeSpec({
  describe("KMongo Repository Visitor Tests") {
    it("Can generate a simple repository") {
      verifyGeneratedCode(
        source = Specs.domainWithDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "T004__repository_simple.txt",
      )
    }
    it("Can generate a repository for a nested document") {
      verifyGeneratedCode(
        source = Specs.domainWithNestedDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "T005__repository_nested.txt",
      )
    }
    it("Can generate a repository for a deeply nested document") {
      verifyGeneratedCode(
        source = Specs.domainWithDeeplyNestedDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "T006__repository_deeply_nested.txt",
      )
    }
    it("Can generate a repository with a unique index") {
      verifyGeneratedCode(
        source = Specs.domainWithUniqueIndex,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "T007__repository_unique_index.txt",
      )
    }
    it("Can generate a repository with a composite index") {
      verifyGeneratedCode(
        source = Specs.domainWithCompositeIndex,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "T008__repository_composite_index.txt",
      )
    }
  }
})
