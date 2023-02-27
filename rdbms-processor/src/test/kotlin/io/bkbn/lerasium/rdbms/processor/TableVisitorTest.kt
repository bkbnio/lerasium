package io.bkbn.lerasium.rdbms.processor

import io.bkbn.lerasium.utils.TestUtils.verifyGeneratedCode
import io.kotest.core.spec.style.DescribeSpec

class TableVisitorTest : DescribeSpec({
  describe("Table Generation") {
    it("Can construct a simple table with scalar types") {
      verifyGeneratedCode(
        source = "spec/001__spec_simple_types.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserTable.kt",
        fileSnapshot = "snapshot/T001__table_with_scalar_fields.txt",
      )
    }
    // TODO
    xit("Can override the column name") {
      verifyGeneratedCode(
        source = "spec/007__spec_with_column_name_override.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserTable.kt",
        fileSnapshot = "snapshot/T003__table_with_column_name_override.txt",
      )
    }
    it("Can construct a table with nullable fields") {
      verifyGeneratedCode(
        source = "spec/008__spec_with_nullable_field.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "LettersTable.kt",
        fileSnapshot = "snapshot/T008__table_with_nullable_fields.txt",
      )
    }
    it("Can construct a table with a foreign key reference") {
      verifyGeneratedCode(
        source = "spec/009__spec_with_foreign_key.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserTable.kt",
        fileSnapshot = "snapshot/T012__table_with_foreign_key_reference.txt",
      )
    }
    // TODO
    xit("Can construct a table with a one-to-many reference") {
      verifyGeneratedCode(
        source = "spec/010__spec_with_one_to_many_reference.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        fileUnderTest = "UserTable.kt",
        fileSnapshot = "snapshot/T013__table_with_one_to_many_reference.txt",
      )
    }
    xit("Can construct table with a many-to-many reference") {
      verifyGeneratedCode(
        source = "spec/011__spec_with_many_to_many_reference.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 6,
        filesUnderTest = mapOf(
          "BookTable.kt" to "snapshot/T014__table_with_many_to_many_reference__book_table.txt",
          "BookReviewTable.kt" to "snapshot/T014__table_with_many_to_many_reference__book_review_table.txt",
        )
      )
    }
    it("Can construct multiple tables in a single source set") {
      verifyGeneratedCode(
        source = "spec/012__spec_with_multiple_domains.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 5,
        filesUnderTest = mapOf(
          "OtherWordsTable.kt" to "snapshot/T015__table_with_many_to_many_reference__other_words_table.txt",
          "WordsTable.kt" to "snapshot/T015__table_with_many_to_many_reference__words_table.txt",
        )
      )
    }
    // TODO
    xit("Properly masks a sensitive field") {
      verifyGeneratedCode(
        source = "spec/013__spec_with_sensitive_field.txt",
        provider = RdbmsProcessorProvider(),
        expectedFileCount = 3,
        fileUnderTest = "UserTable.kt",
        fileSnapshot = "snapshot/T021__table_with_sensitive_field.txt",
      )
    }
  }
})
