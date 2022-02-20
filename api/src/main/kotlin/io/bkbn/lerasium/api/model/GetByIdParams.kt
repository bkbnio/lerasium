package io.bkbn.lerasium.api.model

import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import java.util.UUID

data class GetByIdParams(
  @Param(ParamType.PATH)
  val id: UUID
)
