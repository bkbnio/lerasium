package io.bkbn.lerasium.utils

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.serialization.Serializers
import io.bkbn.lerasium.utils.StringUtils.camelToSnakeCase
import kotlinx.serialization.Serializable

object KotlinPoetUtils {

  private const val BASE_PACKAGE_NAME = "io.bkbn.lerasium.generated"
  const val BASE_API_PACKAGE_NAME = "$BASE_PACKAGE_NAME.api"
  const val BASE_MODEL_PACKAGE_NAME = "$BASE_PACKAGE_NAME.models"
  const val BASE_ENTITY_PACKAGE_NAME = "$BASE_PACKAGE_NAME.entity"

  fun CodeBlock.Builder.addControlFlow(
    controlFlow: String,
    vararg args: Any,
    init: CodeBlock.Builder.() -> Unit
  ) {
    beginControlFlow(controlFlow, *args)
    add(CodeBlock.Builder().apply(init).build())
    endControlFlow()
  }

  fun CodeBlock.Builder.addObjectInstantiation(
    type: TypeName,
    trailingComma: Boolean = false,
    init: CodeBlock.Builder.() -> Unit
  ) {
    add("%T(\n", type)
    indent()
    add(CodeBlock.Builder().apply(init).build())
    unindent()
    when (trailingComma) {
      true -> add("),\n")
      false -> add(")\n")
    }
  }

  fun FunSpec.Builder.addCodeBlock(
    init: CodeBlock.Builder.() -> Unit
  ) {
    addCode(CodeBlock.builder().apply(init).build())
  }

  fun Domain.toTocClass(): ClassName = ClassName(BASE_API_PACKAGE_NAME, name.plus("ToC"))
  fun Domain.toCreateRequestClass(): ClassName = ClassName(BASE_MODEL_PACKAGE_NAME, name.plus("CreateRequest"))
  fun Domain.toUpdateRequestClass(): ClassName = ClassName(BASE_MODEL_PACKAGE_NAME, name.plus("UpdateRequest"))
  fun Domain.toResponseClass(): ClassName = ClassName(BASE_MODEL_PACKAGE_NAME, name.plus("Response"))
  fun Domain.toEntityClass(): ClassName = ClassName(BASE_ENTITY_PACKAGE_NAME, name.plus("Entity"))
  fun Domain.toTableClass(): ClassName = ClassName(BASE_ENTITY_PACKAGE_NAME, name.plus("Table"))
  fun Domain.toDaoClass(): ClassName = ClassName(BASE_ENTITY_PACKAGE_NAME, name.plus("Dao"))
  fun Domain.toApiDocumentationClass(): ClassName = ClassName(BASE_API_PACKAGE_NAME, name.plus("ApiDocs"))
  fun Domain.toAuthTag(): String = "jwt_auth_${name.camelToSnakeCase()}"

  fun String.toResponseClass(): ClassName = ClassName(BASE_MODEL_PACKAGE_NAME, this.plus("Response"))
  fun String.toEntityClass(): ClassName = ClassName(BASE_ENTITY_PACKAGE_NAME, this.plus("Entity"))

  fun ClassName.toEntityClass(): ClassName = ClassName(BASE_ENTITY_PACKAGE_NAME, simpleName.plus("Entity"))

  fun KSPropertyDeclaration.toParameter() = ParameterSpec.builder(simpleName.getShortName(), type.toTypeName()).build()

  fun KSPropertyDeclaration.toProperty(isMutable: Boolean = false) =
    PropertySpec.builder(simpleName.getShortName(), type.toTypeName()).apply {
      if (type.resolve().toClassName().simpleName == "UUID") {
        addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
          addMember("with = %T::class", Serializers.Uuid::class)
        }.build())
      }
      if (isMutable) mutable()
      initializer(simpleName.getShortName())
    }.build()

  fun KSTypeReference.isSupportedScalar(): Boolean = when (this.resolve().toClassName().simpleName) {
    "String" -> true
    "Int" -> true
    "Long" -> true
    "Double" -> true
    "Float" -> true
    "Boolean" -> true
    "UUID" -> true
    else -> false
  }
}
