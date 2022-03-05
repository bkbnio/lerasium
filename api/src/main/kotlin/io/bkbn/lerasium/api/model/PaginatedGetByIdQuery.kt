package io.bkbn.lerasium.api.model

import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import java.util.UUID

data class PaginatedGetByIdQuery(
  @Param(ParamType.PATH)
  val id: UUID,
  @Param(ParamType.QUERY)
  val chunk: Int = 100,
  @Param(ParamType.QUERY)
  val offset: Int = 0
)
