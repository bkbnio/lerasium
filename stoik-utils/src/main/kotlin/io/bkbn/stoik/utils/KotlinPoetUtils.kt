package io.bkbn.stoik.utils

import com.squareup.kotlinpoet.CodeBlock

object KotlinPoetUtils {
  fun CodeBlock.Builder.addControlFlow(
    controlFlow: String,
    vararg args: Any,
    init: CodeBlock.Builder.() -> Unit
  ) {
    beginControlFlow(controlFlow, *args)
    val block = CodeBlock.Builder()
    block.init()
    add(block.build())
    endControlFlow()
  }
}
