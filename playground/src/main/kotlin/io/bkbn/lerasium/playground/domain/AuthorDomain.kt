package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.rdbms.OneToMany
import io.bkbn.lerasium.rdbms.Table

@Domain("Author")
internal sealed interface AuthorDomain {
  val name: String
  @Relation
  val books: BookDomain
}

@Table
internal interface AuthorTableSpec : AuthorDomain {
  @OneToMany("author")
  override val books: BookDomain
}

@Api
internal interface AuthorApiSpec : AuthorDomain
