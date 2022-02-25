package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.rdbms.Table

@Domain("Book")
private sealed interface BookDomain {
  val title: String
  val isbn: String
  val rating: Double
}

@Table
private interface BookTable : BookDomain {
  @Index(unique = true)
  override val isbn: String
}

@Api
private interface BookApi : BookDomain
