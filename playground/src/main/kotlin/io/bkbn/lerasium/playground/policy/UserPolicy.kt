package io.bkbn.lerasium.playground.policy

import io.bkbn.bouncer.core.Policy
import io.bkbn.bouncer.core.bouncerPolicy
import io.bkbn.lerasium.core.policy.PolicyProvider
import io.bkbn.lerasium.core.policy.action.CrudAction
import io.bkbn.lerasium.generated.entity.UserEntity

object UserPolicy : PolicyProvider<UserEntity, CrudAction, UserEntity> {
  override fun get(): Policy<UserEntity, CrudAction, UserEntity> = bouncerPolicy {
    can("create user", CrudAction.CREATE, UserEntity::class) { _, _ -> true }
    can("read user", CrudAction.READ, UserEntity::class) { e, r -> e.id == r.id }
    can("update user", CrudAction.UPDATE, UserEntity::class) { e, r -> e.id == r.id }
    can("delete user", CrudAction.DELETE, UserEntity::class) { e, r -> e.id == r.id }
  }
}
