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
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getCollectionType
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.LerasiumUtils.getDomainOrNull
import io.bkbn.lerasium.utils.LerasiumUtils.isCollection
import io.bkbn.lerasium.utils.LerasiumUtils.isDomain

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
        .forEach {
          nestedDomainVisitor.visitTypeReference(
            it.type, NestedDomainVisitor.Data(parentCharter = charter)
          )
        }
    }.build())
  }

  private fun TypeSpec.Builder.domainPrimaryConstructor(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
      .filter {
        it.type.isDomain()
          || it.type.isSupportedScalar()
          || (it.type.isCollection() && it.type.getCollectionType().isDomain())
      }
    val nestedProps = charter.classDeclaration.getAllProperties()
      .filterNot { it.type.isSupportedScalar() }
      .filterNot { it.isAnnotationPresent(Relation::class) }
      .filterNot { it.type.isDomain() }
      .filterNot { it.type.isCollection() && it.type.getCollectionType().isDomain() }
    primaryConstructor(FunSpec.constructorBuilder().apply {
      properties.forEach {
        val param = when (it.type.isSupportedScalar()) {
          true -> it.toParameter()
          false -> {
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
          }
        }
        addParameter(param)
      }
      nestedProps.forEach { prop ->
        val n = prop.simpleName.getShortName()
        val t = prop.type.resolve().toClassName()
        addParameter(ParameterSpec.builder(n, t).build())
      }
    }.build())
  }

  @Suppress("NestedBlockDepth")
  private fun TypeSpec.Builder.domainProperties(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
      .filter {
        it.type.isDomain()
          || it.type.isSupportedScalar()
          || (it.type.isCollection() && it.type.getCollectionType().isDomain())
      }
    val nestedProps = charter.classDeclaration.getAllProperties()
      .filterNot { it.type.isSupportedScalar() }
      .filterNot { it.isAnnotationPresent(Relation::class) }
      .filterNot { it.type.isDomain() }
      .filterNot { it.type.isCollection() && it.type.getCollectionType().isDomain() }
    properties.forEach {
      val prop = when (it.type.isSupportedScalar()) {
        true -> it.toProperty(isOverride = true, serializable = false)
        false -> {
          val n = it.simpleName.getShortName()
          val domain = it.getDomainOrNull()
          if (domain != null) {
            val domainType = it.getDomainType()
            PropertySpec.builder(n, domainType).apply {
              addModifiers(KModifier.OVERRIDE)
              initializer(n)
            }.build()
          } else {
            val tn = it.type.resolve().declaration.simpleName.getShortName()
            val pn = charter.classDeclaration.toClassName().canonicalName
            val t = ClassName(pn, tn)
            PropertySpec.builder(n, t).apply {
              addModifiers(KModifier.OVERRIDE)
              initializer(n)
            }.build()
          }
        }
      }
      addProperty(prop)
    }
    nestedProps.forEach { prop ->
      val n = prop.simpleName.getShortName()
      val t = prop.type.resolve().toClassName()
      addProperty(PropertySpec.builder(n, t).apply {
        addModifiers(KModifier.OVERRIDE)
        initializer(n)
      }.build())
    }
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
