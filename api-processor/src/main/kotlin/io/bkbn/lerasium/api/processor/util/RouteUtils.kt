package io.bkbn.lerasium.api.processor.util

import com.squareup.kotlinpoet.CodeBlock

object RouteUtils {
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
