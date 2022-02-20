package io.bkbn.lerasium.core.validation

import io.bkbn.lerasium.core.Domain
import io.konform.validation.Constraint
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder

object DomainValidation {
  val constraints = Validation<Domain> {
    Domain::name {
      pascalCase()
    }
  }

  private fun ValidationBuilder<String>.pascalCase(): Constraint<String> {
    return addConstraint("Must be UpperCamelCase syntax") { it.matches(Regex("^([A-Z][a-z]*)+")) }
  }
}
