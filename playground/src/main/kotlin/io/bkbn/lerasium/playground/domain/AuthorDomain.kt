package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.rdbms.OneToMany
import io.bkbn.lerasium.rdbms.Table

@Domain("Author")
internal sealed interface AuthorDomain {
  val name: String
}

// public val books: SizedIterable<BookEntity> by BookEntity referrersOn BookTable.author

@Table
internal interface AuthorTableSpec : AuthorDomain {
  @OneToMany("author")
  val books: BookTable
}

@Api
internal interface AuthorApiSpec : AuthorDomain
