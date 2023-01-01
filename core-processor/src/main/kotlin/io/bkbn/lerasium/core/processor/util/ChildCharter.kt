package io.bkbn.lerasium.core.processor.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.bkbn.lerasium.utils.LerasiumCharter

class ChildCharter(
  classDeclaration: KSClassDeclaration,
  parentCharter: LerasiumCharter
) : LerasiumCharter(parentCharter.domain, classDeclaration) {
  override val isActor: Boolean = parentCharter.isActor
}
