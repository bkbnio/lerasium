package io.bkbn.stoik.playground.spec

import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.exposed.Column
import io.bkbn.stoik.exposed.Table
import io.bkbn.stoik.exposed.Unique
import io.bkbn.stoik.ktor.core.Api
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

@Table("user")
interface UserTableSpec : UserSpec {
  @Column("first_name")
  override val firstName: String

  @Column("last_name")
  override val lastName: String

  @Unique
  override val email: String
}

@Api
interface UserApiSpec: UserSpec

object UUIDSerializer : KSerializer<UUID> {
  override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): UUID {
    return UUID.fromString(decoder.decodeString())
  }

  override fun serialize(encoder: Encoder, value: UUID) {
    encoder.encodeString(value.toString())
  }
}
