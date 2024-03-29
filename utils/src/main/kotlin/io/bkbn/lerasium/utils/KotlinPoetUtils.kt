package io.bkbn.lerasium.utils

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.auth.RbacPolicyProvider
import io.bkbn.lerasium.core.serialization.Serializers
import io.bkbn.lerasium.utils.LerasiumUtils.getCollectionType
import io.bkbn.lerasium.utils.LerasiumUtils.isCollection
import io.bkbn.lerasium.utils.LerasiumUtils.isDomain
import kotlinx.serialization.Serializable

object KotlinPoetUtils {

  private const val BASE_PACKAGE_NAME = "io.bkbn.lerasium.generated"
  private const val BASE_API_PACKAGE_NAME = "$BASE_PACKAGE_NAME.api"
  private const val BASE_PERSISTENCE_PACKAGE_NAME = "$BASE_PACKAGE_NAME.persistence"

  // API
  const val API_DOCS_PACKAGE_NAME = "$BASE_API_PACKAGE_NAME.docs"
  const val API_CONTROLLER_PACKAGE_NAME = "$BASE_API_PACKAGE_NAME.controller"
  const val API_MODELS_PACKAGE_NAME = "$BASE_API_PACKAGE_NAME.models"
  const val API_SERVICE_PACKAGE_NAME = "$BASE_API_PACKAGE_NAME.service"
  const val API_CONFIG_PACKAGE_NAME = "$BASE_API_PACKAGE_NAME.config"

  // Domain
  const val DOMAIN_PACKAGE_NAME = "$BASE_PACKAGE_NAME.domain"

  // Policy
  const val POLICY_PACKAGE_NAME = "$BASE_PACKAGE_NAME.policy"

  // Persistence
  const val REPOSITORY_PACKAGE_NAME = "$BASE_PERSISTENCE_PACKAGE_NAME.repository"
  const val TABLE_PACKAGE_NAME = "$BASE_PERSISTENCE_PACKAGE_NAME.table"
  const val DOCUMENT_PACKAGE_NAME = "$BASE_PERSISTENCE_PACKAGE_NAME.document"
  const val PERSISTENCE_CONFIG_PACKAGE_NAME = "$BASE_PERSISTENCE_PACKAGE_NAME.config"

  fun CodeBlock.Builder.addControlFlow(
    controlFlow: String,
    vararg args: Any,
    init: CodeBlock.Builder.() -> Unit
  ) {
    beginControlFlow(controlFlow, *args)
    add(CodeBlock.Builder().apply(init).build())
    endControlFlow()
  }

  fun CodeBlock.Builder.addControlFlowWithTrailingComma(
    controlFlow: String,
    vararg args: Any,
    init: CodeBlock.Builder.() -> Unit
  ) {
    beginControlFlow(controlFlow, *args)
    add(CodeBlock.Builder().apply(init).build())
    unindent()
    add("},\n")
  }

  fun CodeBlock.Builder.addObjectInstantiation(
    type: ClassName,
    trailingComma: Boolean = false,
    returnInstance: Boolean = false,
    assignment: String? = null,
    init: CodeBlock.Builder.() -> Unit
  ) {
    if (returnInstance) add("return %T(\n", type)
    else if (assignment != null) add("%L = %T(\n", assignment, type)
    else add("%T(\n", type)
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

  fun Domain.toEntityClass(): ClassName = ClassName(TABLE_PACKAGE_NAME, name.plus("Entity"))
  fun Domain.toTableClass(): ClassName = ClassName(TABLE_PACKAGE_NAME, name.plus("Table"))
  fun Domain.toRepositoryClass(): ClassName = ClassName(REPOSITORY_PACKAGE_NAME, name.plus("Repository"))
  fun Domain.toApiDocumentationClass(): ClassName = ClassName(API_DOCS_PACKAGE_NAME, name.plus("Documentation"))

  fun String.toResponseClass(): ClassName = ClassName(API_MODELS_PACKAGE_NAME, this.plus("Response"))
  fun String.toEntityClass(): ClassName = ClassName(TABLE_PACKAGE_NAME, this.plus("Entity"))

  fun ClassName.toEntityClass(): ClassName = ClassName(TABLE_PACKAGE_NAME, simpleName.plus("Entity"))

  fun KSPropertyDeclaration.toParameter(guaranteeNullable: Boolean = false) =
    ParameterSpec.builder(
      simpleName.getShortName(),
      type.toTypeName().copy(nullable = guaranteeNullable || this.type.resolve().isMarkedNullable)
    ).build()

  fun KSPropertyDeclaration.toProperty(
    isMutable: Boolean = false,
    isOverride: Boolean = false,
    serializable: Boolean = true
  ) =
    PropertySpec.builder(simpleName.getShortName(), type.toTypeName()).apply {
      if (type.resolve().toClassName().simpleName == "UUID" && serializable) {
        addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
          addMember("with = %T::class", Serializers.Uuid::class)
        }.build())
      }
      if (isMutable) mutable()
      if (isOverride) addModifiers(KModifier.OVERRIDE)
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

  fun KSTypeReference.isEnum(): Boolean = (resolve().declaration as KSClassDeclaration).classKind.name == "ENUM_CLASS"

  @OptIn(KspExperimental::class)
  fun KSClassDeclaration.collectProperties(): PropertyWrapper {
    val scalars = getAllProperties()
      .filterNot { it.isPolicy() }
      .filter { it.type.isSupportedScalar() }
    val relations = getAllProperties().filter { it.isAnnotationPresent(Relation::class) }
    // TODO Cleaner way?
    val nestedProps = getAllProperties()
      .filterNot { it.isPolicy() }
      .filterNot { it.type.isSupportedScalar() }
      .filterNot { it.isAnnotationPresent(Relation::class) }
      .filterNot { it.type.isDomain() }
      .filterNot { it.type.isCollection() && it.type.getCollectionType().isDomain() }
      .filterNot { it.type.isEnum() }
    val enums = getAllProperties().filter { it.type.isEnum() }
    return PropertyWrapper(
      scalars = scalars,
      relations = relations,
      nested = nestedProps,
      enums = enums
    )
  }

  private fun KSPropertyDeclaration.isPolicy(): Boolean = type.resolve().toClassName().let {
    it == RbacPolicyProvider::class.asClassName()
  }
}
