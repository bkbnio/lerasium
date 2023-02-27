package io.bkbn.lerasium.rdbms.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class ConfigFileTest : DescribeSpec({
  describe("Config File") {
    it("Can create a config file") {
      verifyGeneratedCode(
        source = "spec/001__spec_simple_types.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "PostgresConfig.kt",
        fileSnapshot = "snapshot/T020__config_file.txt",
      )
    }
  }
})
