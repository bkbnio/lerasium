package io.bkbn.stoik.playground.domain

import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.exposed.Table
import io.bkbn.stoik.ktor.Api

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
