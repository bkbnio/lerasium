package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Sensitive
import io.bkbn.lerasium.persistence.CompositeIndex
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.rdbms.Table

@Domain("User")
internal sealed interface UserDomain {
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
internal interface UserTable : UserDomain {
  @Index(true)
  override val email: String
}

@Api
internal interface UserApi: UserDomain
