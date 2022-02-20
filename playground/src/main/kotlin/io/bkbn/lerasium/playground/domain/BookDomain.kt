package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.rdbms.Table
import io.bkbn.lerasium.api.Api

@Domain("Book")
private sealed interface BookDomain {
  val title: String
  val isbn: String
  val rating: Double
}

@Table
private interface BookTable : BookDomain

@Api
private interface BookApi : BookDomain
