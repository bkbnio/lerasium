package io.bkbn.lerasium.mongo.processor

import io.bkbn.lerasium.mongo.processor.Specs.domainWithDeeplyNestedDocument
import io.bkbn.lerasium.mongo.processor.Specs.domainWithDocument
import io.bkbn.lerasium.mongo.processor.Specs.domainWithNestedDocument
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class DocumentVisitorTest : DescribeSpec({
  describe("KMongo Document Visitor Tests") {
    it("Can generate a simple document") {
      verifyGeneratedCode(
        source = domainWithDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDocument.kt",
        fileSnapshot = "T001__document_simple.txt",
      )
    }
    it("Can generate a simple nested document") {
      verifyGeneratedCode(
        source = domainWithNestedDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDocument.kt",
        fileSnapshot = "T002__document_nested.txt",
      )
    }
    it("Can generate a deeply nested document") {
      verifyGeneratedCode(
        source = domainWithDeeplyNestedDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 2,
        fileUnderTest = "UserDocument.kt",
        fileSnapshot = "T003__document_deeply_nested.txt",
      )
    }
  }
})
