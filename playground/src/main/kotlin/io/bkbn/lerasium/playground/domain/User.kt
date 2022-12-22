package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Sensitive
import io.bkbn.lerasium.core.policy.Policy
import io.bkbn.lerasium.persistence.CompositeIndex
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.playground.policy.UserPolicy
import io.bkbn.lerasium.rdbms.Table

@Domain("User")
@Policy(provider = UserPolicy::class)
internal sealed interface User {
  val firstName: String
  val lastName: String
  val email: String
  val favoriteFood: String?

  @Sensitive
  val password: String
}

@Table
@CompositeIndex(unique = false, "firstName", "lastName")
@CompositeIndex(unique = false, "favoriteFood", "lastName")
internal interface UserTable : User {
  @Index(true)
  override val email: String

  @Index
  override val favoriteFood: String?
}

@Api
internal interface UserApi : User {
  @GetBy(true)
  override val email: String

  @GetBy
  override val favoriteFood: String?
}
