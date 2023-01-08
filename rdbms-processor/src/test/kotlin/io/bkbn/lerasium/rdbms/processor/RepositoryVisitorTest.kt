package io.bkbn.lerasium.rdbms.processor

import io.bkbn.lerasium.rdbms.processor.Specs.domainWithBasicTypes
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithForeignKeyReference
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithManyToManyReference
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithOneToManyReference
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class RepositoryVisitorTest : DescribeSpec({
  describe("Repository Visitor") {
    it("Can create a repository for a simple domain") {
      verifyGeneratedCode(
        source = domainWithBasicTypes,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "T016__repository_for_simple_domain.txt",
      )
    }
    it("Can create a repository for a domain with a foreign key") {
      verifyGeneratedCode(
        source = domainWithForeignKeyReference,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "T017__repository_for_domain_with_foreign_key.txt",
      )
    }
    it("Can create a repository for a domain with a one-to-many relationship") {
      verifyGeneratedCode(
        source = domainWithOneToManyReference,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "CountryRepository.kt",
        fileSnapshot = "T018__repository_for_domain_with_one_to_many.txt",
      )
    }
    xit("Can create a repository for a domain with a many-to-many relationship") {
      verifyGeneratedCode(
        source = domainWithManyToManyReference,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "CountryRepository.kt",
        fileSnapshot = "T019__repository_for_domain_with_many_to_many.txt",
      )
    }
  }
})
