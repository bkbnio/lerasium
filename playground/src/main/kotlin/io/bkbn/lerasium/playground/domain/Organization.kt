package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.rdbms.ManyToMany
import io.bkbn.lerasium.rdbms.Table

@Api
@Domain("Organization")
@Table
interface Organization {
  val name: String
}
