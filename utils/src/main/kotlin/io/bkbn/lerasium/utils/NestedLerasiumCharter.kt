package io.bkbn.lerasium.utils

import com.google.devtools.ksp.symbol.KSClassDeclaration

class NestedLerasiumCharter(
  classDeclaration: KSClassDeclaration,
  parentCharter: LerasiumCharter,
) : LerasiumCharter(parentCharter.domain, classDeclaration) {
  override val isActor: Boolean = parentCharter.isActor
}
