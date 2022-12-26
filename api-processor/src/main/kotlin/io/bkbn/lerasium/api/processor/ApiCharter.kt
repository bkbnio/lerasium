package io.bkbn.lerasium.api.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.core.Actor
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.utils.LerasiumUtils.findParent

class ApiCharter(val domain: Domain, val cd: KSClassDeclaration) {
  @OptIn(KspExperimental::class)
  val isActor: Boolean = cd.findParent().isAnnotationPresent(Actor::class)
  @OptIn(KspExperimental::class)
  val hasQueries: Boolean = cd.getAllProperties().any { it.isAnnotationPresent(GetBy::class) }
}
