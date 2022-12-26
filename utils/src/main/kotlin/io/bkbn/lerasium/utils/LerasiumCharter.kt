package io.bkbn.lerasium.utils

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.bkbn.lerasium.core.auth.Actor
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.utils.LerasiumUtils.findParent

class LerasiumCharter(val domain: Domain, val cd: KSClassDeclaration) {
  @OptIn(KspExperimental::class)
  val isActor: Boolean = cd.findParent().isAnnotationPresent(Actor::class)
}
