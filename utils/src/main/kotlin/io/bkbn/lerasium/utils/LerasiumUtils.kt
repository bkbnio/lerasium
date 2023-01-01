package io.bkbn.lerasium.utils

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.validation.DomainValidation

@OptIn(KspExperimental::class)
object LerasiumUtils {

  fun KSClassDeclaration.getDomain(): Domain {
    return this.getAnnotationsByType(Domain::class).firstOrNull()?.let {
      val domainValidation = DomainValidation.constraints.validate(it)
      require(domainValidation.errors.isEmpty()) { "Domain is invalid ${domainValidation.errors}" }
      it
    } ?: error("$this is not annotated with a valid domain!")
  }

  fun KSTypeReference.getDomain(): Domain =
    (resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).first()

  fun KSClassDeclaration.findParentDomain(): Domain {
    val domainType = superTypes
      .map { t -> t.resolve().declaration as KSClassDeclaration }
      .find { t -> t.isAnnotationPresent(Domain::class) }
      ?: error("Must implement an interface annotated with Domain")
    val domain = domainType.getAnnotationsByType(Domain::class).first()
    val domainValidation = DomainValidation.constraints.validate(domain)
    require(domainValidation.errors.isEmpty()) { "Domain is invalid ${domainValidation.errors}" }
    return domain
  }

  fun KSClassDeclaration.findParent(): KSClassDeclaration {
    return superTypes
      .map { t -> t.resolve().declaration as KSClassDeclaration }
      .find { t -> t.isAnnotationPresent(Domain::class) }
      ?: error("Must implement an interface annotated with Domain")
  }

  fun KSClassDeclaration.findParentNullable(): KSClassDeclaration? {
    return superTypes
      .map { t -> t.resolve().declaration as KSClassDeclaration }
      .find { t -> t.isAnnotationPresent(Domain::class) }
  }

}
