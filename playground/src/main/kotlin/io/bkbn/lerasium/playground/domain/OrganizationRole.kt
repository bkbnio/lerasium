package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.model.ModelProvider
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.Table
import kotlinx.serialization.Serializable
import java.util.UUID

@Api
@Domain("OrganizationRole")
@Table
interface OrganizationRole {
  @ForeignKey
  val organization: ModelProvider<UUID, Organization>
  @ForeignKey
  val user: ModelProvider<UUID, User>
  val role: Type

  @Serializable
  enum class Type {
    ADMIN,
    MAINTAINER,
    CONTRIBUTOR
  }
}
