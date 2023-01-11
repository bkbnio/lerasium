@file:OptIn(KspExperimental::class)

package io.bkbn.lerasium.core.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
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
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.utils.KotlinPoetUtils.isEnum
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getCollectionType
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.LerasiumUtils.getDomainOrNull
import io.bkbn.lerasium.utils.LerasiumUtils.isCollection
import io.bkbn.lerasium.utils.LerasiumUtils.isDomain
import kotlinx.serialization.Serializable

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
      charter.classDeclaration.getAllProperties()
        .filterNot { it.type.isSupportedScalar() }
        .filterNot { it.type.isCollection() }
        .filterNot { (it.type.resolve().declaration as KSClassDeclaration).isAnnotationPresent(Domain::class) }
        .filterNot { it.type.isEnum() }
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
      properties.domain.map {
        val n = it.simpleName.getShortName()
        val domain = it.getDomainOrNull()
        if (domain != null) {
          val domainType = it.getDomainType()
          ParameterSpec.builder(n, domainType).build()
        } else {
          val tn = it.type.resolve().declaration.simpleName.getShortName()
          val pn = charter.classDeclaration.toClassName().canonicalName
          val t = ClassName(pn, tn)
          ParameterSpec.builder(n, t).build()
        }
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
    properties.domain.map {
      val n = it.simpleName.getShortName()
      val domain = it.getDomainOrNull()
      if (domain != null) {
        val domainType = it.getDomainType()
        PropertySpec.builder(n, domainType).apply {
          addModifiers(KModifier.OVERRIDE)
          initializer(n)
        }.build()
      } else {
        // TODO Does this ever get hit?
        val tn = it.type.resolve().declaration.simpleName.getShortName()
        val pn = charter.classDeclaration.toClassName().canonicalName
        val t = ClassName(pn, tn)
        PropertySpec.builder(n, t).apply {
          addModifiers(KModifier.OVERRIDE)
          initializer(n)
        }.build()
      }
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

  private fun KSClassDeclaration.collectProperties(): Properties {
    val scalars = getAllProperties().filter { it.type.isSupportedScalar() }
    val domain = getAllProperties().filter {
      it.type.isDomain() || (it.type.isCollection() && it.type.getCollectionType().isDomain())
    }
    // TODO Cleaner way?
    val nestedProps = getAllProperties()
      .filterNot { it.type.isSupportedScalar() }
      .filterNot { it.isAnnotationPresent(Relation::class) }
      .filterNot { it.type.isDomain() }
      .filterNot { it.type.isCollection() && it.type.getCollectionType().isDomain() }
      .filterNot { it.type.isEnum() }
    val enums = getAllProperties().filter { it.type.isEnum() }
    return Properties(scalars, domain, nestedProps, enums)
  }

  private data class Properties(
    val scalars: Sequence<KSPropertyDeclaration>,
    val domain: Sequence<KSPropertyDeclaration>,
    val nested: Sequence<KSPropertyDeclaration>,
    val enums: Sequence<KSPropertyDeclaration>
  )
}
