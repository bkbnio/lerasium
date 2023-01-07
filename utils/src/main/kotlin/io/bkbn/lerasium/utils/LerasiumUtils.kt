package io.bkbn.lerasium.utils

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.validation.DomainValidation
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain

@OptIn(KspExperimental::class)
object LerasiumUtils {

  fun KSClassDeclaration.getDomain(): Domain {
    return this.getAnnotationsByType(Domain::class).firstOrNull()?.let {
      val domainValidation = DomainValidation.constraints.validate(it)
      require(domainValidation.errors.isEmpty()) { "Domain is invalid ${domainValidation.errors}" }
      it
    } ?: error("$this is not annotated with a valid domain!")
  }

  fun KSTypeReference.getDomain(): Domain = getDomainOrNull() ?: error("Domain cannot be null")

  fun KSTypeReference.getDomainOrNull(): Domain? =
    (resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).firstOrNull()

  private fun KSTypeReference.asClassDeclaration() = resolve().declaration as KSClassDeclaration

  fun KSTypeReference.isDomain(): Boolean =
    (resolve().declaration as KSClassDeclaration).isAnnotationPresent(Domain::class)

  fun KSTypeReference.isCollection(): Boolean =
    this.asClassDeclaration().qualifiedName?.asString()?.startsWith("kotlin.collections") ?: false

  fun KSPropertyDeclaration.getCollectionType() = type.getCollectionType()

  fun KSTypeReference.getCollectionType() = resolve().arguments.firstOrNull()?.type
    ?: error("Error resolving collection type for ${toTypeName()}")
}
