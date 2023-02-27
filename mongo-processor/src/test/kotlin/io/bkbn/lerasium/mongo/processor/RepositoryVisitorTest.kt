package io.bkbn.lerasium.mongo.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class RepositoryVisitorTest : DescribeSpec({
  describe("KMongo Repository Visitor Tests") {
    it("Can generate a simple repository") {
      verifyGeneratedCode(
        source = "spec/001__domain_with_document.txt",
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "snapshot/001__repository_simple.txt",
      )
    }
    it("Can generate a repository for a nested document") {
      verifyGeneratedCode(
        source = "spec/002__domain_with_nested_document.txt",
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "snapshot/002__repository_nested.txt",
      )
    }
    it("Can generate a repository for a deeply nested document") {
      verifyGeneratedCode(
        source = "spec/003__domain_with_deeply_nested_document.txt",
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "snapshot/003__repository_deeply_nested.txt",
      )
    }
    it("Can generate a repository with a unique index") {
      verifyGeneratedCode(
        source = "spec/004__domain_with_unique_index.txt",
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "snapshot/004__repository_unique_index.txt",
      )
    }
    it("Can generate a repository with a composite index") {
      verifyGeneratedCode(
        source = "spec/005__domain_with_composite_index.txt",
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "snapshot/005__repository_composite_index.txt",
      )
    }
  }
})
