package io.bkbn.lerasium.api.util

import io.bkbn.kompendium.json.schema.definition.TypeDefinition
import io.bkbn.kompendium.oas.payload.Parameter

object ApiDocumentationUtils {
  fun getAllParameters() = listOf(
    Parameter(
      name = "chunk",
      `in` = Parameter.Location.query,
      schema = TypeDefinition.INT,
      description = "The maximum number of entities that can be considered a page",
    ),
    Parameter(
      name = "offset",
      `in` = Parameter.Location.query,
      schema = TypeDefinition.INT,
      description = "The page you wish to return"
    )
  )

  fun idParameter() = listOf(
    Parameter(
      name = "id",
      `in` = Parameter.Location.path,
      schema = TypeDefinition.UUID,
      description = "The ID of the entity"
    )
  )
}
