package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.model.DomainProvider
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.Table
import kotlinx.serialization.Serializable
import java.util.UUID

@Api
@Domain("OrganizationRole")
@Table
interface OrganizationRole {

  @Relation
  @ForeignKey
  val organization: DomainProvider<UUID, Organization>

  @Relation
  @ForeignKey
  val user: DomainProvider<UUID, User>

  val role: Type

  @Serializable
  enum class Type {
    ADMIN,
    MAINTAINER,
    CONTRIBUTOR
  }
}
