package io.bkbn.stoik.exposed.processor.util

object StringHelpers {
  private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
  private val snakeRegex = "_[a-zA-Z]".toRegex()

  fun String.camelToSnakeCase(): String {
    return camelRegex.replace(this) {
      "_${it.value}"
    }.toLowerCase()
  }

  fun String.snakeToLowerCamelCase(): String {
    return snakeRegex.replace(this) {
      it.value.replace("_","")
        .toUpperCase()
    }
  }

  fun String.snakeToUpperCamelCase(): String {
    return this.snakeToLowerCamelCase().capitalize()
  }

}
