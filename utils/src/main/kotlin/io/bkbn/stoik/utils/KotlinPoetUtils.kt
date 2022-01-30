package io.bkbn.stoik.utils

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import io.bkbn.stoik.core.Domain

object KotlinPoetUtils {

  const val BASE_MODEL_PACKAGE_NAME = "io.bkbn.stoik.generated.models"
  const val BASE_ENTITY_PACKAGE_NAME = "io.bkbn.stoik.generated.entity"

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

  fun Domain.toCreateRequestClass(): ClassName = ClassName(BASE_MODEL_PACKAGE_NAME, name.plus("CreateRequest"))
  fun Domain.toUpdateRequestClass(): ClassName = ClassName(BASE_MODEL_PACKAGE_NAME, name.plus("UpdateRequest"))
  fun Domain.toResponseClass(): ClassName = ClassName(BASE_MODEL_PACKAGE_NAME, name.plus("Response"))
  fun Domain.toEntityClass(): ClassName = ClassName(BASE_ENTITY_PACKAGE_NAME, name.plus("Entity"))
  fun Domain.toDaoClass(): ClassName = ClassName(BASE_ENTITY_PACKAGE_NAME, name.plus("Dao"))
}
