package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.PrimaryKey
import io.bkbn.lerasium.rdbms.Table

/*
object BookReviews : Table("book_reviews") {
  val reader = reference("reader", UserTable)
  val book = reference("book", BookTable)
  val rating = integer("rating")
  val review = text("review")
  override val primaryKey = PrimaryKey(reader, book)
}
 */
@Domain("BookReview")
internal sealed interface BookReview {
  val reader: UserDomain
  val book: BookDomain
  val rating: Int
  val review: String
}

@Table
@PrimaryKey("reader", "book")
internal interface BookReviewTable : BookReview {
  @ForeignKey
  override val reader: UserDomain
  @ForeignKey
  override val book: BookDomain
}

@Api
internal interface BookReviewApi: BookReview
