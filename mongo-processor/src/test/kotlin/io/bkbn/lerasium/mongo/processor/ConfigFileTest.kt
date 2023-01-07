package io.bkbn.lerasium.mongo.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class ConfigFileTest : DescribeSpec({
  describe("KMongo Config File Tests") {
    it("Can generate a config file") {
      verifyGeneratedCode(
        source = Specs.domainWithDocument,
        provider = KMongoProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "MongoConfig.kt",
        fileSnapshot = "T009__config_file.txt",
      )
    }
  }
})
