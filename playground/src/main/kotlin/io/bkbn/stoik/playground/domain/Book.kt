package io.bkbn.stoik.playground.domain

import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.exposed.Table
import io.bkbn.stoik.ktor.Api

@Domain("Book")
sealed interface Book {
  val title: String
  val isbn: String
  val rating: Double
}

@Table
interface BookTable : Book

@Api
interface BookApi : Book
