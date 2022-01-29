package io.bkbn.stoik.core.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.stoik.core.model.Request
import io.bkbn.stoik.core.model.Response
import io.bkbn.stoik.utils.StoikUtils.getDomain
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@OptIn(KotlinPoetKspPreview::class)
class ModelVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {
  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Domain", classDeclaration)
      return
    }

    val domainName = classDeclaration.getDomain().name

    fileBuilder.addCreateRequestModel(classDeclaration, domainName)
    fileBuilder.addUpdateRequestModel(classDeclaration, domainName)
    fileBuilder.addResponseModel(classDeclaration, domainName)
  }

  private fun FileSpec.Builder.addCreateRequestModel(cd: KSClassDeclaration, name: String) {
    val properties = cd.getAllProperties().toList()
    val thisClazz = ClassName(this@addCreateRequestModel.packageName, name.plus("CreateRequest"))
    // TODO Hack, cannot stay like this
    val entityClazz = ClassName("io.bkbn.stoik.generated.table", name.plus("Entity"))
    addType(TypeSpec.classBuilder(name.plus("CreateRequest")).apply {
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(Request.Create::class.asTypeName().parameterizedBy(thisClazz, entityClazz))
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach { addParameter(it.toParameter()) }
      }.build())
      properties.forEach { addProperty(it.toProperty()) }
      addFunction(FunSpec.builder("toEntity").apply {
        addModifiers(KModifier.OVERRIDE)
        receiver(thisClazz)
        returns(entityClazz)
        addStatement("TODO(%S)", "Not yet implemented")
      }.build())
    }.build())
  }

  private fun FileSpec.Builder.addUpdateRequestModel(cd: KSClassDeclaration, name: String) {
    val properties = cd.getAllProperties().toList()
    val thisClazz = ClassName(this@addUpdateRequestModel.packageName, name.plus("UpdateRequest"))
    // TODO Hack, cannot stay like this
    val entityClazz = ClassName("io.bkbn.stoik.generated.table", name.plus("Entity"))
    addType(TypeSpec.classBuilder(name.plus("UpdateRequest")).apply {
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(Request.Update::class.asTypeName().parameterizedBy(thisClazz, entityClazz))
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach {
          addParameter(
            ParameterSpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).build()
          )
        }
      }.build())
      properties.forEach {
        addProperty(
          PropertySpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).apply {
            initializer(it.simpleName.getShortName())
          }.build()
        )
      }
      addFunction(FunSpec.builder("toEntity").apply {
        addModifiers(KModifier.OVERRIDE)
        receiver(thisClazz)
        returns(entityClazz)
        addStatement("TODO(%S)", "Not yet implemented")
      }.build())
    }.build())
  }

  private fun FileSpec.Builder.addResponseModel(cd: KSClassDeclaration, name: String) {
    val properties = cd.getAllProperties().toList()
    addType(TypeSpec.classBuilder(name.plus("Response")).apply {
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addModifiers(KModifier.DATA)
      addSuperinterface(Response::class.asTypeName())
      primaryConstructor(FunSpec.constructorBuilder().apply {
        addParameter(ParameterSpec("id", UUID::class.asTypeName()))
        properties.forEach { addParameter(it.toParameter()) }
        addParameter(ParameterSpec("createdAt", LocalDateTime::class.asTypeName()))
        addParameter(ParameterSpec("updatedAt", LocalDateTime::class.asTypeName()))
      }.build())
      properties.forEach { addProperty(it.toProperty()) }
      addProperty(PropertySpec.builder("id", UUID::class.asTypeName()).apply {
        addAnnotation(Contextual::class)
        initializer("id")
      }.build())
      addProperty(PropertySpec.builder("createdAt", LocalDateTime::class.asTypeName()).apply {
        initializer("createdAt")
      }.build())
      addProperty(PropertySpec.builder("updatedAt", LocalDateTime::class.asTypeName()).apply {
        initializer("updatedAt")
      }.build())
    }.build())
  }

  private fun KSPropertyDeclaration.toParameter() =
    ParameterSpec.builder(simpleName.getShortName(), type.toTypeName()).build()

  private fun KSPropertyDeclaration.toProperty() =
    PropertySpec.builder(simpleName.getShortName(), type.toTypeName()).apply {
      initializer(simpleName.getShortName())
    }.build()
}
