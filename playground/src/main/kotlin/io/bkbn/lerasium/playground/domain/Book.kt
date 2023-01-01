package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.ManyToMany
import io.bkbn.lerasium.rdbms.Table

@Api
@Domain("Book")
interface Book {
  val title: String
  @GetBy(unique = true)
  val isbn: String
  val rating: Double
  val author: Author
  @Relation
  val readers: User
}

@Table
internal interface BookTable : Book {
  @Index(unique = true)
  override val isbn: String

  @ForeignKey
  override val author: Author

  @ManyToMany(BookReview::class)
  override val readers: User
}
