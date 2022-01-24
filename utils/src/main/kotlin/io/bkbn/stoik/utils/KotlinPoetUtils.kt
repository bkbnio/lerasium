package io.bkbn.stoik.utils

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

object KotlinPoetUtils {
  fun CodeBlock.Builder.addControlFlow(
    controlFlow: String,
    vararg args: Any,
    init: CodeBlock.Builder.() -> Unit
  ) {
    beginControlFlow(controlFlow, *args)
    add(CodeBlock.Builder().apply(init).build())
    endControlFlow()
  }

  fun FunSpec.Builder.addCodeBlock(
    init: CodeBlock.Builder.() -> Unit
  ) {
    addCode(CodeBlock.builder().apply(init).build())
  }
}
