@file:OptIn(KspExperimental::class)

package io.bkbn.lerasium.core.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.utils.KotlinPoetUtils.collectProperties
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getCollectionType
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.LerasiumUtils.getDomainOrNull
import io.bkbn.lerasium.utils.LerasiumUtils.isCollection

class RootDomainVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {
  private lateinit var containingFile: KSFile

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Domain", classDeclaration)
      return
    }

    containingFile = classDeclaration.containingFile!!

    val domain = classDeclaration.getDomain()
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addDomainModels(charter)
  }

  private fun FileSpec.Builder.addDomainModels(charter: LerasiumCharter) {
    addType(TypeSpec.classBuilder(charter.domain.name.plus("Domain")).apply {
      addSuperinterface(charter.classDeclaration.toClassName())
      addModifiers(KModifier.DATA)
      domainPrimaryConstructor(charter)
      domainProperties(charter)

      val nestedDomainVisitor = NestedDomainVisitor(this, logger)
      val properties = charter.classDeclaration.collectProperties()
      properties.nested
        .forEach {
          nestedDomainVisitor.visitTypeReference(
            it.type, NestedDomainVisitor.Data(parentCharter = charter)
          )
        }
    }.build())
  }

  private fun TypeSpec.Builder.domainPrimaryConstructor(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.collectProperties()
    primaryConstructor(FunSpec.constructorBuilder().apply {
      properties.scalars.map { it.toParameter() }.forEach { addParameter(it) }
      properties.relations.map {
        val n = it.simpleName.getShortName()
        ParameterSpec.builder(n,  it.type.toTypeName()).build()
      }.forEach { addParameter(it) }
      properties.nested.map { prop ->
        val n = prop.simpleName.getShortName()
        val t = prop.type.resolve().toClassName()
        ParameterSpec.builder(n, t).build()
      }.forEach { addParameter(it) }
      properties.enums.map { prop ->
        val n = prop.simpleName.getShortName()
        val t = prop.type.resolve().toClassName()
        // TODO Check if serializable
        ParameterSpec.builder(n, t).build()
      }.forEach { addParameter(it) }
    }.build())
  }

  @Suppress("NestedBlockDepth")
  private fun TypeSpec.Builder.domainProperties(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.collectProperties()
    properties.scalars.map { it.toProperty(isOverride = true, serializable = false) }.forEach { addProperty(it) }
    properties.relations.map {
      val n = it.simpleName.getShortName()
      PropertySpec.builder(n,  it.type.toTypeName()).apply {
        addModifiers(KModifier.OVERRIDE)
        initializer(n)
      }.build()
    }.forEach { addProperty(it) }
    properties.nested.map { prop ->
      val n = prop.simpleName.getShortName()
      val t = prop.type.resolve().toClassName()
      PropertySpec.builder(n, t).apply {
        addModifiers(KModifier.OVERRIDE)
        initializer(n)
      }.build()
    }.forEach { addProperty(it) }
    properties.enums.map { prop ->
      val n = prop.simpleName.getShortName()
      val t = prop.type.resolve().toClassName()
      // TODO Check if serializable
      PropertySpec.builder(n, t).apply {
        addModifiers(KModifier.OVERRIDE)
        initializer(n)
      }.build()
    }.forEach { addProperty(it) }
  }

  private fun KSPropertyDeclaration.getDomainOrNull() = if (type.isCollection()) {
    type.getCollectionType().getDomainOrNull()
  } else {
    type.getDomainOrNull()
  }

  private fun KSPropertyDeclaration.getDomainType() = if (type.isCollection()) {
    List::class.asTypeName().parameterizedBy(type.getCollectionType().toTypeName())
  } else {
    type.toTypeName()
  }
}
