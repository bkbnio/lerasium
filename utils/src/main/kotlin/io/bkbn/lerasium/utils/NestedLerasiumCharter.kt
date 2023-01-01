package io.bkbn.lerasium.utils

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.bkbn.lerasium.utils.LerasiumCharter

class NestedLerasiumCharter(
  classDeclaration: KSClassDeclaration,
  parentCharter: LerasiumCharter
) : LerasiumCharter(parentCharter.domain, classDeclaration) {
  override val isActor: Boolean = parentCharter.isActor
}
