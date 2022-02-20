package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.rdbms.Table
import io.bkbn.lerasium.rdbms.Unique
import io.bkbn.lerasium.api.Api

@Domain("User")
private sealed interface UserDomain {
  val firstName: String
  val lastName: String
  val email: String
}

@Table
private interface UserTable : UserDomain {
  @Unique
  override val email: String
}

@Api
private interface UserApi: UserDomain
