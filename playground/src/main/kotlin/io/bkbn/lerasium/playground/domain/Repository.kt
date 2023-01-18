package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.domain.LerasiumDomain
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.Table

@Api
@Domain("Repository")
@Table
interface Repository : LerasiumDomain {
  val name: String
  val isPublic: Boolean
  @ForeignKey
  val organization: Organization
}
