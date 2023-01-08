package io.bkbn.lerasium.core.converter

interface ConvertTo<A> {
  suspend fun to(): A
}
