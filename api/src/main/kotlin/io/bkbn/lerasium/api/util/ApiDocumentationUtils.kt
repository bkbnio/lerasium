package io.bkbn.lerasium.api.util

import io.bkbn.kompendium.json.schema.definition.TypeDefinition
import io.bkbn.kompendium.oas.payload.Parameter

object ApiDocumentationUtils {
  fun getAllParameters() = listOf(
    Parameter(
      name = "chunk",
      `in` = Parameter.Location.query,
      schema = TypeDefinition.INT
    ),
    Parameter(
      name = "offset",
      `in` = Parameter.Location.query,
      schema = TypeDefinition.INT
    )
  )
}
