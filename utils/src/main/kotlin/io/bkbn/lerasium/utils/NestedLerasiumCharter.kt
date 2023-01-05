package io.bkbn.lerasium.utils

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName

class NestedLerasiumCharter(
  classDeclaration: KSClassDeclaration,
  parentCharter: LerasiumCharter,
) : LerasiumCharter(parentCharter.domain, classDeclaration) {
  override val isActor: Boolean = parentCharter.isActor
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
}
