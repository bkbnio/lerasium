package io.bkbn.stoik.playground.models

import io.bkbn.stoik.exposed.Column
import io.bkbn.stoik.exposed.Sensitive
import io.bkbn.stoik.exposed.Table

sealed interface UserSpec {
  val firstName: String
  val lastName: String
  val email: String
  val password: String
}

@Table("user")
interface UserTableSpec : UserSpec {
  @Column("first_name")
  override val firstName: String

  @Column("last_name")
  override val lastName: String

  @Sensitive
  override val password: String
}

@Suppress("EmptyClassBlock")
interface UserApiSpec : UserSpec {
}
