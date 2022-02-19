package io.bkbn.stoik.playground.domain

import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.exposed.Table
import io.bkbn.stoik.exposed.Unique
import io.bkbn.stoik.ktor.Api

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
