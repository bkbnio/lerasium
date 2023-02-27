package io.bkbn.lerasium.rdbms.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class RepositoryVisitorTest : DescribeSpec({
  describe("Repository Visitor") {
    it("Can create a repository for a simple domain") {
      verifyGeneratedCode(
        source = "spec/001__spec_simple_types.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "snapshot/T016__repository_for_simple_domain.txt",
      )
    }
    it("Can create a repository for a domain with a foreign key") {
      verifyGeneratedCode(
        source = "spec/002__spec_with_foreign_key.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserRepository.kt",
        fileSnapshot = "snapshot/T017__repository_for_domain_with_foreign_key.txt",
      )
    }
    it("Can create a repository for a domain with a one-to-many relationship") {
      verifyGeneratedCode(
        source = "spec/003__spec_with_one_to_many_relationship.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "CountryRepository.kt",
        fileSnapshot = "snapshot/T018__repository_for_domain_with_one_to_many.txt",
      )
    }
    xit("Can create a repository for a domain with a many-to-many relationship") {
      verifyGeneratedCode(
        source = "spec/004__spec_with_many_to_many_relationship.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "CountryRepository.kt",
        fileSnapshot = "snapshot/T019__repository_for_domain_with_many_to_many.txt",
      )
    }
    it("Can create the necessary query for a rbac policy") {
      verifyGeneratedCode(
        source = "spec/005__spec_with_rbac_policy.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 7,
        fileUnderTest = "OrganizationRepository.kt",
        fileSnapshot = "snapshot/T022__repository_with_rbac_policy.txt",
      )
    }
    it("Can create the necessary query for a rbac policy on foreign key") {
      verifyGeneratedCode(
        source = "spec/006__spec_with_rbac_foreign_key.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 9,
        fileUnderTest = "RepositoryRepository.kt",
        fileSnapshot = "snapshot/T023__repository_with_rbac_policy_on_foreign_key.txt",
      )
    }
  }
})
