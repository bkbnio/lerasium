package io.bkbn.lerasium.core.auth

import io.bkbn.bouncer.core.AbacPolicy
import io.bkbn.bouncer.core.RbacPolicy

sealed interface PolicyProvider

interface RbacPolicyProvider<Actor : Any, Action : Enum<*>, RoleResource : Any, Role : Enum<*>, Resource : Any> {
  val policy: RbacPolicy<Actor, Action, Role, Resource>
}


interface AbacPolicyProvider<Actor : Any, Action : Enum<*>, Resource : Any> {
  val policy: AbacPolicy<Actor, Action, Resource>
}
