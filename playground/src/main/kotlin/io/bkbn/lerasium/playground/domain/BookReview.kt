package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.persistence.CompositeIndex
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.Table

@Domain("BookReview")
internal sealed interface BookReview {
  val reader: User
  val book: Book
  val rating: Int
  val review: String
}

@Table
@CompositeIndex(true, "reader", "book")
internal interface BookReviewTable : BookReview {
  @ForeignKey
  override val reader: User

  @ForeignKey
  override val book: Book
}

@Api
internal interface BookReviewApi : BookReview
