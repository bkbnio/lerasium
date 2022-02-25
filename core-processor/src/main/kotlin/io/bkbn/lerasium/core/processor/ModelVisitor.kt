package io.bkbn.lerasium.core.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Sensitive
import io.bkbn.lerasium.core.model.Request
import io.bkbn.lerasium.core.model.Response
import io.bkbn.lerasium.core.serialization.Serializers
import io.bkbn.lerasium.utils.KotlinPoetUtils.BASE_MODEL_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.util.UUID

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
class ModelVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  private lateinit var containingFile: KSFile

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Domain", classDeclaration)
      return
    }

    containingFile = classDeclaration.containingFile!!

    val domain = classDeclaration.getDomain()

    fileBuilder.addCreateRequestModel(classDeclaration, domain.name)
    fileBuilder.addUpdateRequestModel(classDeclaration, domain.name)
    fileBuilder.addResponseModel(classDeclaration, domain.name, true)

    classDeclaration.getAllProperties().toList()
      .filterNot { it.type.isSupportedScalar() }
      .forEach { visitTypeReference(it.type, Unit) }
  }

  override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
    val simpleName = typeReference.resolve().toClassName().simpleName
    val classDeclaration = typeReference.resolve().declaration as KSClassDeclaration

    classDeclaration.getAllProperties().toList()
      .filterNot { it.type.isSupportedScalar() }
      .forEach { visitTypeReference(it.type, Unit) }

    fileBuilder.addCreateRequestModel(classDeclaration, simpleName)
    fileBuilder.addUpdateRequestModel(classDeclaration, simpleName)
    fileBuilder.addResponseModel(classDeclaration, simpleName)
  }

  private fun FileSpec.Builder.addCreateRequestModel(cd: KSClassDeclaration, name: String) {
    val properties = cd.getAllProperties().toList()
    addType(TypeSpec.classBuilder(name.plus("CreateRequest")).apply {
      addOriginatingKSFile(containingFile)
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(Request.Create::class)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> it.toParameter()
            false -> {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus("CreateRequest")
              val cn = ClassName(BASE_MODEL_PACKAGE_NAME, t)
              ParameterSpec.builder(n, cn).build()
            }
          }
          addParameter(param)
        }
      }.build())
      properties.forEach {
        val prop = when (it.type.isSupportedScalar()) {
          true -> it.toProperty()
          false -> {
            val n = it.simpleName.getShortName()
            val t = it.type.resolve().toClassName().simpleName.plus("CreateRequest")
            val cn = ClassName(BASE_MODEL_PACKAGE_NAME, t)
            PropertySpec.builder(n, cn).apply {
              initializer(n)
            }.build()
          }
        }
        addProperty(prop)
      }
    }.build())
  }

  private fun FileSpec.Builder.addUpdateRequestModel(cd: KSClassDeclaration, name: String) {
    val properties = cd.getAllProperties().toList()
    addType(TypeSpec.classBuilder(name.plus("UpdateRequest")).apply {
      addOriginatingKSFile(containingFile)
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(Request.Update::class)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> {
              ParameterSpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).build()
            }
            false -> {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus("UpdateRequest")
              val cn = ClassName(BASE_MODEL_PACKAGE_NAME, t).copy(nullable = true)
              ParameterSpec.builder(n, cn).build()
            }
          }
          addParameter(param)
        }
      }.build())
      properties.forEach {
        val prop = when (it.type.isSupportedScalar()) {
          true -> {
            PropertySpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).apply {
              initializer(it.simpleName.getShortName())
            }.build()
          }
          false -> {
            val n = it.simpleName.getShortName()
            val t = it.type.resolve().toClassName().simpleName.plus("UpdateRequest")
            val cn = ClassName(BASE_MODEL_PACKAGE_NAME, t).copy(nullable = true)
            PropertySpec.builder(n, cn).apply {
              initializer(n)
            }.build()
          }
        }
        addProperty(prop)
      }
    }.build())
  }

  private fun FileSpec.Builder.addResponseModel(cd: KSClassDeclaration, name: String, isDomainModel: Boolean = false) {
    val responseProperties = cd.getAllProperties().filterNot { it.isAnnotationPresent(Sensitive::class) }
    addType(TypeSpec.classBuilder(name.plus("Response")).apply {
      addOriginatingKSFile(containingFile)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addModifiers(KModifier.DATA)
      addSuperinterface(Response::class.asTypeName())
      primaryConstructor(FunSpec.constructorBuilder().apply {
        if (isDomainModel) addParameter(ParameterSpec("id", UUID::class.asTypeName()))
        responseProperties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> it.toParameter()
            false -> {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus("Response")
              val cn = ClassName(BASE_MODEL_PACKAGE_NAME, t)
              ParameterSpec.builder(n, cn).build()
            }
          }
          addParameter(param)
        }
        if (isDomainModel) addParameter(ParameterSpec("createdAt", LocalDateTime::class.asTypeName()))
        if (isDomainModel) addParameter(ParameterSpec("updatedAt", LocalDateTime::class.asTypeName()))
      }.build())
      responseProperties.forEach {
        val prop = when (it.type.isSupportedScalar()) {
          true -> it.toProperty()
          false -> {
            val n = it.simpleName.getShortName()
            val t = it.type.resolve().toClassName().simpleName.plus("Response")
            val cn = ClassName(BASE_MODEL_PACKAGE_NAME, t)
            PropertySpec.builder(n, cn).apply {
              initializer(n)
            }.build()
          }
        }
        addProperty(prop)
      }
      if (isDomainModel) {
        addProperty(PropertySpec.builder("id", UUID::class.asTypeName()).apply {
          addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
            addMember("with = %T::class", Serializers.Uuid::class)
          }.build())
          initializer("id")
        }.build())
        addProperty(PropertySpec.builder("createdAt", LocalDateTime::class.asTypeName()).apply {
          initializer("createdAt")
        }.build())
        addProperty(PropertySpec.builder("updatedAt", LocalDateTime::class.asTypeName()).apply {
          initializer("updatedAt")
        }.build())
      }
    }.build())
  }
}
