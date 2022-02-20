package io.bkbn.lerasium.utils

import java.util.Locale

object StringUtils {
  private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
  private val snakeRegex = "_[a-zA-Z]".toRegex()

  fun String.camelToSnakeCase(): String {
    return camelRegex.replace(this) {
      "_${it.value}"
    }.lowercase(Locale.getDefault())
  }

  fun String.pascalToSnakeCase(): String {
    return "((?<=.)[A-Z][a-zA-Z]*)|((?<=[a-zA-Z])\\d+)".toRegex().replace(this) {
      "_${it.value}"
    }.lowercase(Locale.getDefault())
  }

  fun String.snakeToLowerCamelCase(): String {
    return snakeRegex.replace(this) {
      it.value.replace("_", "")
        .uppercase(Locale.getDefault())
    }
  }

  fun String.snakeToUpperCamelCase(): String {
    return this.snakeToLowerCamelCase()
      .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
  }

  fun String.capitalized() =
    this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

  fun String.decapitalized() =
    replaceFirstChar { it.lowercase(Locale.getDefault()) }
}
