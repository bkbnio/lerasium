package io.bkbn.stoik.utils

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.core.validation.DomainValidation

@OptIn(KspExperimental::class)
object StoikUtils {

  fun KSClassDeclaration.findValidDomain(): Domain {
    val domainType = superTypes
      .map { t -> t.resolve().declaration }
      .find { t -> t.isAnnotationPresent(Domain::class) }
      ?: error("Api must implement an interface annotated with Domain")
    val domain = domainType.getAnnotationsByType(Domain::class).first()
    val domainValidation = DomainValidation.constraints.validate(domain)
    require(domainValidation.errors.isEmpty()) { "Domain is invalid ${domainValidation.errors}" }
    return domain
  }

}
