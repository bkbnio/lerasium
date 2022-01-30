package io.bkbn.stoik.playground.spec

import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.exposed.Column
import io.bkbn.stoik.exposed.Table
import io.bkbn.stoik.exposed.Unique
import io.bkbn.stoik.ktor.Api
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

@Domain("User")
sealed interface UserSpec {
  val firstName: String
  val lastName: String
  val email: String
}

@Table
interface UserTableSpec : UserSpec {
  @Unique
  override val email: String
}

@Api
interface UserApiSpec: UserSpec
