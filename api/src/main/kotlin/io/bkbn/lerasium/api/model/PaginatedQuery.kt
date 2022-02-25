package io.bkbn.lerasium.api.model

import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType

data class PaginatedQuery(
  @Param(ParamType.QUERY)
  val chunk: Int = 100,
  @Param(ParamType.QUERY)
  val offset: Long = 0
)
