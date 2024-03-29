package io.bkbn.lerasium.utils

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import io.bkbn.lerasium.utils.KotlinPoetUtils.DOMAIN_PACKAGE_NAME

class NestedLerasiumCharter(
  classDeclaration: KSClassDeclaration,
  parentCharter: LerasiumCharter,
) : LerasiumCharter(parentCharter.domain, classDeclaration) {
  override val isActor: Boolean = parentCharter.isActor

  override val domainClass: ClassName = classDeclaration.toClassName()
  val domainClassImpl: ClassName = when (parentCharter) {
    is NestedLerasiumCharter -> ClassName(
      parentCharter.domainClassImpl.canonicalName,
      classDeclaration.simpleName.getShortName()
    )

    else -> ClassName(DOMAIN_PACKAGE_NAME, domain.name.plus("Domain"), classDeclaration.simpleName.getShortName())
  }

  // TBH Not sure why I had to do this, but it works
  override val apiResponseClass: ClassName = when (parentCharter) {
    is NestedLerasiumCharter -> ClassName(
      parentCharter.ioModelClass.canonicalName,
      parentCharter.classDeclaration.simpleName.asString(),
      classDeclaration.simpleName.asString(),
      "Response"
    )

    else -> ClassName(
      parentCharter.ioModelClass.canonicalName,
      classDeclaration.simpleName.asString(),
      "Response"
    )
  }

  override val documentClass: ClassName = ClassName(
    parentCharter.documentClass.canonicalName,
    classDeclaration.simpleName.asString().plus("Document")
  )
}
