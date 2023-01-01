package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.persistence.CompositeIndex
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.Table

@Api
@Domain("BookReview")
@Table
@CompositeIndex(true, "reader", "book")
interface BookReview {
  @ForeignKey
  val reader: User
  @ForeignKey
  val book: Book
  val rating: Int
  val review: String
}
