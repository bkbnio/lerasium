package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.domain.LerasiumDomain
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.ManyToMany
import io.bkbn.lerasium.rdbms.Table
import java.util.UUID

@Api
@Domain("Book")
@Table
interface Book : LerasiumDomain {
  val title: String
  @Index(unique = true)
  @GetBy(unique = true)
  val isbn: String
  val rating: Double
  @ForeignKey
  val author: Author
  @ManyToMany(BookReview::class)
  @Relation
  val readers: List<User>
}
