package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.rdbms.OneToMany
import io.bkbn.lerasium.rdbms.Table

@Domain("Author")
internal sealed interface Author {
  val name: String
  @Relation
  val books: Book
}

@Table
internal interface AuthorTableSpec : Author {
  @OneToMany("author")
  override val books: Book
}

@Api
internal interface AuthorApiSpec : Author
