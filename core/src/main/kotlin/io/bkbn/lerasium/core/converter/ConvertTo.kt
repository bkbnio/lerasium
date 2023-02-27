package io.bkbn.lerasium.core.converter

import io.bkbn.lerasium.core.request.RequestContext

interface ConvertTo<A> {
  suspend fun to(): A
}
