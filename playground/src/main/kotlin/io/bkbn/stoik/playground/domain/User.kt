package io.bkbn.stoik.playground.domain

import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.exposed.Table
import io.bkbn.stoik.exposed.Unique
import io.bkbn.stoik.ktor.Api

@Domain("User")
sealed interface User {
  val firstName: String
  val lastName: String
  val email: String
}

@Table
interface UserTable : User {
  @Unique
  override val email: String
}

@Api
interface UserApi: User
