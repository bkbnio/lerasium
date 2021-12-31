package io.bkbn.stoik.playground.spec

import io.bkbn.stoik.exposed.Column
import io.bkbn.stoik.exposed.Table
import io.bkbn.stoik.exposed.Unique
import io.bkbn.stoik.ktor.core.Api

sealed interface UserSpec {
  val firstName: String
  val lastName: String
  val email: String
}

@Table("user")
interface UserTableSpec : UserSpec {
  @Column("first_name")
  override val firstName: String

  @Column("last_name")
  override val lastName: String

  @Unique
  override val email: String
}

@Api("User")
interface UserApiSpec : UserSpec
