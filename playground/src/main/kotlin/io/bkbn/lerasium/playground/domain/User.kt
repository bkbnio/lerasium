package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.core.auth.Actor
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Sensitive
import io.bkbn.lerasium.core.auth.Password
import io.bkbn.lerasium.core.auth.Username
import io.bkbn.lerasium.persistence.CompositeIndex
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.rdbms.Table

@Api
@Actor
@Domain("User")
interface User {
  val firstName: String
  val lastName: String

  @GetBy(unique = true)
  @Username
  val email: String

  @Password
  @Sensitive
  val password: String

  @GetBy
  val favoriteFood: String?
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
