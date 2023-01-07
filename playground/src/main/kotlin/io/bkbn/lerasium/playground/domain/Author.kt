package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.domain.LerasiumDomain
import io.bkbn.lerasium.rdbms.OneToMany
import io.bkbn.lerasium.rdbms.Table

@Api
@Domain("Author")
@Table
interface Author : LerasiumDomain {
  val name: String

  @Relation
  @OneToMany("author")
  val books: List<Book>
}
