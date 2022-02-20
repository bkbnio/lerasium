package io.bkbn.lerasium.core.validation

import io.bkbn.lerasium.core.Domain
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.core.spec.style.DescribeSpec

class DomainValidationTest : DescribeSpec({
  describe("Validations") {
    it("Fails validation when plain camel case") {
      // arrange
      val domain = Domain("aYo")

      // assert
      DomainValidation.constraints shouldBeInvalid domain
    }
    it("Passes validation with Pascal case") {
      // arrange
      val domain = Domain("AYo")

      // assert
      DomainValidation.constraints shouldBeValid domain
    }
  }
})
