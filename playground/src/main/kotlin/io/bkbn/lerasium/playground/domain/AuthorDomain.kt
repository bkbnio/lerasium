package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.rdbms.Table

@Domain("Author")
internal sealed interface AuthorDomain {
  val name: String
  // todo books on-to-many
}

@Table
internal interface AuthorTableSpec : AuthorDomain

@Api
internal interface AuthorApiSpec : AuthorDomain
