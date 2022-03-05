package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.Table
import java.util.UUID

@Domain("Book")
internal sealed interface BookDomain {
  val title: String
  val isbn: String
  val rating: Double
  val author: AuthorDomain
}

@Table
internal interface BookTable : BookDomain {
  @Index(unique = true)
  override val isbn: String

  @ForeignKey("name")
  override val author: AuthorDomain
}

@Api
internal interface BookApi : BookDomain
