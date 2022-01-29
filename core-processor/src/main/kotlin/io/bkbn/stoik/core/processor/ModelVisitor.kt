package io.bkbn.stoik.core.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.stoik.utils.StoikUtils.getDomain

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
    addType(TypeSpec.classBuilder(name.plus("CreateRequest")).apply {
      addModifiers(KModifier.DATA)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach { addParameter(it.toParameter()) }
      }.build())
      properties.forEach { addProperty(it.toProperty()) }
    }.build())
  }

  private fun FileSpec.Builder.addUpdateRequestModel(cd: KSClassDeclaration, name: String) {
    val properties = cd.getAllProperties().toList()
    addType(TypeSpec.classBuilder(name.plus("UpdateRequest")).apply {
      addModifiers(KModifier.DATA)
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
    }.build())
  }

  private fun FileSpec.Builder.addResponseModel(cd: KSClassDeclaration, name: String) {
    val properties = cd.getAllProperties().toList()
    addType(TypeSpec.classBuilder(name.plus("Response")).apply {
      addModifiers(KModifier.DATA)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach { addParameter(it.toParameter()) }
      }.build())
      properties.forEach { addProperty(it.toProperty()) }
    }.build())
  }

  private fun KSPropertyDeclaration.toParameter() =
    ParameterSpec.builder(simpleName.getShortName(), type.toTypeName()).build()

  private fun KSPropertyDeclaration.toProperty() =
    PropertySpec.builder(simpleName.getShortName(), type.toTypeName()).apply {
      initializer(simpleName.getShortName())
    }.build()
}
