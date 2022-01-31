package io.bkbn.stoik.kmongo.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.core.model.Entity
import io.bkbn.stoik.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.stoik.utils.KotlinPoetUtils.toEntityClass
import io.bkbn.stoik.utils.KotlinPoetUtils.toParameter
import io.bkbn.stoik.utils.KotlinPoetUtils.toProperty
import io.bkbn.stoik.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.stoik.utils.StoikUtils.findParentDomain
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
class DocumentVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    val memberProps = MemberName("kotlin.reflect.full", "memberProperties")
    val valueParams = MemberName("kotlin.reflect.full", "valueParameters")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()
    fileBuilder.addDocument(classDeclaration, domain)
  }

  private fun FileSpec.Builder.addDocument(cd: KSClassDeclaration, domain: Domain) {
    val properties = cd.getAllProperties().toList()
    addType(TypeSpec.classBuilder(domain.name.plus("Entity")).apply {
      addOriginatingKSFile(cd.containingFile!!)
      addSuperinterface(Entity::class.asClassName().parameterizedBy(domain.toResponseClass()))
      addAnnotation(Serializable::class)
      addModifiers(KModifier.DATA)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        addParameter(ParameterSpec("id", UUID::class.asTypeName()))
        properties.forEach { addParameter(it.toParameter()) }
        addParameter(ParameterSpec("createdAt", LocalDateTime::class.asTypeName()))
        addParameter(ParameterSpec("updatedAt", LocalDateTime::class.asTypeName()))
      }.build())
      properties.forEach { addProperty(it.toProperty(true)) }
      addProperty(PropertySpec.builder("id", UUID::class.asTypeName()).apply {
        addAnnotation(Contextual::class)
        addAnnotation(AnnotationSpec.builder(SerialName::class).apply {
          addMember("%S", "_id")
        }.build())
        initializer("id")
      }.build())
      addProperty(PropertySpec.builder("createdAt", LocalDateTime::class.asTypeName()).apply {
        mutable()
        initializer("createdAt")
      }.build())
      addProperty(PropertySpec.builder("updatedAt", LocalDateTime::class.asTypeName()).apply {
        mutable()
        initializer("updatedAt")
      }.build())
      addFunction(FunSpec.builder("toResponse").apply {
        addModifiers(KModifier.OVERRIDE)
        returns(domain.toResponseClass())
        addCode(CodeBlock.builder().apply {
          addControlFlow("return with(::%T)", domain.toResponseClass()) {
            addStatement(
              "val propertiesByName = %T::class.%M.associateBy { it.name }",
              domain.toEntityClass(),
              memberProps
            )
            addControlFlow("val params = %M.associateWith", valueParams) {
              addControlFlow("when (it.name)") {
                addStatement("else -> propertiesByName[it.name]?.get(this@%L)", domain.toEntityClass().simpleName)
              }
            }
            addStatement("callBy(params)")
          }
        }.build())
      }.build())
    }.build())
  }
}
