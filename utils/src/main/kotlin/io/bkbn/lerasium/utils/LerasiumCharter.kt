package io.bkbn.lerasium.utils

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.auth.Actor
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_MODELS_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_SERVICE_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.DAO_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.DOMAIN_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.ENTITY_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.REPOSITORY_PACKAGE_NAME

open class LerasiumCharter(val domain: Domain, val classDeclaration: KSClassDeclaration) {
  @OptIn(KspExperimental::class)
  open val isActor: Boolean = classDeclaration.isAnnotationPresent(Actor::class)
  val domainClass: ClassName = ClassName(DOMAIN_PACKAGE_NAME, domain.name)

  // TODO Should not leak out of persistence layer
  val daoClass: ClassName = ClassName(DAO_PACKAGE_NAME, domain.name.plus("Dao"))
  val repositoryClass: ClassName = ClassName(REPOSITORY_PACKAGE_NAME, domain.name.plus("Repository"))
  val entityClass: ClassName = ClassName(ENTITY_PACKAGE_NAME, domain.name.plus("Entity"))
  val tableClass: ClassName = ClassName(ENTITY_PACKAGE_NAME, domain.name.plus("Table"))

  // TODO Should not leak out of api layer
  val apiServiceClass: ClassName = ClassName(API_SERVICE_PACKAGE_NAME, domain.name.plus("Service"))
  val ioModelClass: ClassName = ClassName(API_MODELS_PACKAGE_NAME, domain.name.plus("IOModels"))
  val apiCreateRequestClass: ClassName = ClassName(ioModelClass.canonicalName, "Create")
  val apiUpdateRequestClass: ClassName = ClassName(ioModelClass.canonicalName, "Update")
  open val apiResponseClass: ClassName = ClassName(ioModelClass.canonicalName, "Response")
}
