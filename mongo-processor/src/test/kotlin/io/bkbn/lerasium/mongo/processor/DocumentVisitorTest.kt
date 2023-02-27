package io.bkbn.lerasium.mongo.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class DocumentVisitorTest : DescribeSpec({
  describe("KMongo Document Visitor Tests") {
    it("Can generate a simple document") {
      verifyGeneratedCode(
        source = "spec/001__domain_with_document.txt",
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserDocument.kt",
        fileSnapshot = "snapshot/007__document_simple.txt",
      )
    }
    it("Can generate a simple nested document") {
      verifyGeneratedCode(
        source = "spec/002__domain_with_nested_document.txt",
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserDocument.kt",
        fileSnapshot = "snapshot/008__document_nested.txt",
      )
    }
    it("Can generate a deeply nested document") {
      verifyGeneratedCode(
        source = "spec/003__domain_with_deeply_nested_document.txt",
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserDocument.kt",
        fileSnapshot = "snapshot/009__document_deeply_nested.txt",
      )
    }
  }
})
