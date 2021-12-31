package io.bkbn.stoik.dao.core

fun interface FromEntity<A : Any, E : Any> {
  fun E.fromEntity(): A
}
