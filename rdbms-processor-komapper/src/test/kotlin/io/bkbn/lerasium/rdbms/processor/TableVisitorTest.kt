package io.bkbn.lerasium.rdbms.processor

import io.bkbn.lerasium.rdbms.processor.Specs.domainWitColumnNameOverride
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithBasicTypes
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithCompositeIndexedField
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithCustomVarcharSize
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithForeignKeyReference
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithIndexedField
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithManyToManyReference
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithNullableFields
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithOneToManyReference
import io.bkbn.lerasium.rdbms.processor.Specs.domainWithUniqueIndexedField
import io.bkbn.lerasium.rdbms.processor.Specs.multipleDomains
import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class TableVisitorTest : DescribeSpec({
  describe("Table Generation") {
    it("Can construct a simple table with scalar types") {
      verifyGeneratedCode(
        source = domainWithBasicTypes,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserTable.kt",
        fileSnapshot = "T001__table_with_scalar_fields.txt",
      )
    }
    it("Can override the column name") {
      verifyGeneratedCode(
        source = domainWitColumnNameOverride,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserTable.kt",
        fileSnapshot = "T003__table_with_column_name_override.txt",
      )
    }
    it("Can construct a varchar with a custom size") {
      verifyGeneratedCode(
        source = domainWithCustomVarcharSize,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "WordsTable.kt",
        fileSnapshot = "T007__table_with_custom_varchar_size.txt",
      )
    }
    it("Can construct a table with nullable fields") {
      verifyGeneratedCode(
        source = domainWithNullableFields,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "LettersTable.kt",
        fileSnapshot = "T008__table_with_nullable_fields.txt",
      )
    }
    it("Can construct a table with an indexed field") {
      verifyGeneratedCode(
        source = domainWithIndexedField,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "WordsTable.kt",
        fileSnapshot = "T009__table_with_indexed_field.txt",
      )
    }
    it("can construct a table with a unique index field") {
      verifyGeneratedCode(
        source = domainWithUniqueIndexedField,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "WordsTable.kt",
        fileSnapshot = "T010__table_with_unique_indexed_field.txt",
      )
    }
    it("Can construct a table with a composite index") {
      verifyGeneratedCode(
        source = domainWithCompositeIndexedField,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "WordsTable.kt",
        fileSnapshot = "T011__table_with_composite_index.txt",
      )
    }
    it("Can construct a table with a foreign key reference") {
      verifyGeneratedCode(
        source = domainWithForeignKeyReference,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserTable.kt",
        fileSnapshot = "T012__table_with_foreign_key_reference.txt",
      )
    }
    it("Can construct a table with a one-to-many reference") {
      verifyGeneratedCode(
        source = domainWithOneToManyReference,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "CountryTable.kt",
        fileSnapshot = "T013__table_with_one_to_many_reference.txt",
      )
    }
    xit("Can construct table with a many-to-many reference") {
      verifyGeneratedCode(
        source = domainWithManyToManyReference,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 6,
        filesUnderTest = mapOf(
          "BookTable.kt" to "T014__table_with_many_to_many_reference__book_table.txt",
          "BookReviewTable.kt" to "T014__table_with_many_to_many_reference__book_review_table.txt",
        )
      )
    }
    it("Can construct multiple tables in a single source set") {
      verifyGeneratedCode(
        source = multipleDomains,
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        filesUnderTest = mapOf(
          "OtherWordsTable.kt" to "T015__table_with_many_to_many_reference__other_words_table.txt",
          "WordsTable.kt" to "T015__table_with_many_to_many_reference__words_table.txt",
        )
      )
    }
  }
})
