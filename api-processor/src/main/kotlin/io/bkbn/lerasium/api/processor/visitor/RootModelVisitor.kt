package io.bkbn.lerasium.api.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.Sensitive
import io.bkbn.lerasium.core.converter.ConvertFrom
import io.bkbn.lerasium.core.model.IORequest
import io.bkbn.lerasium.core.model.IOResponse
import io.bkbn.lerasium.core.serialization.Serializers
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_MODELS_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import kotlinx.serialization.Serializable
import java.util.UUID

@OptIn(KspExperimental::class)
class RootModelVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  private lateinit var containingFile: KSFile

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Domain", classDeclaration)
      return
    }

    containingFile = classDeclaration.containingFile!!

    val domain = classDeclaration.getDomain()
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addIoModelObject(charter)
  }

  private fun FileSpec.Builder.addIoModelObject(charter: LerasiumCharter) {
    addType(TypeSpec.objectBuilder(charter.ioModelClass).apply {
      addCreateRequestModel(charter)
      addUpdateRequestModel(charter)
      addResponseModel(charter)

      val nestedModelVisitor = NestedModelVisitor(this, logger)

      charter.classDeclaration.getAllProperties().toList()
        .filterNot { it.type.isSupportedScalar() }
        .filterNot { (it.type.resolve().declaration as KSClassDeclaration).isAnnotationPresent(Domain::class) }
        .forEach {
          nestedModelVisitor.visitTypeReference(
            it.type, NestedModelVisitor.Data(parentCharter = charter)
          )
        }
    }.build())
  }

  private fun TypeSpec.Builder.addCreateRequestModel(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
      .filterNot { it.simpleName.getShortName() == "id" }
      .filterNot { it.isAnnotationPresent(Relation::class) }
    addType(TypeSpec.classBuilder("Create").apply {
      addOriginatingKSFile(containingFile)
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(IORequest.Create::class)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> it.toParameter()
            false -> {
              val n = it.simpleName.getShortName()
              val domain =
                (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).firstOrNull()
              if (domain != null) {
                ParameterSpec.builder(n, UUID::class).apply {
                  addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
                    addMember("with = %T::class", Serializers.Uuid::class)
                  }.build())
                }.build()
              } else {
                val t = it.type.resolve().toClassName().simpleName.plus(".Create")
                val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
                ParameterSpec.builder(n, cn).build()
              }
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
            val domain =
              (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).firstOrNull()
            if (domain != null) {
              PropertySpec.builder(n, UUID::class).apply {
                initializer(n)
              }.build()
            } else {
              val t = it.type.resolve().toClassName().simpleName.plus(".Create")
              val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
              PropertySpec.builder(n, cn).apply {
                initializer(n)
              }.build()
            }
          }
        }
        addProperty(prop)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateRequestModel(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
      .filterNot { it.simpleName.getShortName() == "id" }
      .filterNot { it.isAnnotationPresent(Relation::class) }
    addType(TypeSpec.classBuilder("Update").apply {
      addOriginatingKSFile(containingFile)
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(IORequest.Update::class)
      updatePrimaryConstructor(properties)
      properties.forEach {
        val prop = when (it.type.isSupportedScalar()) {
          true -> {
            PropertySpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).apply {
              if (it.type.resolve().toClassName().simpleName == "UUID") {
                addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
                  addMember("with = %T::class", Serializers.Uuid::class)
                }.build())
              }
              initializer(it.simpleName.getShortName())
            }.build()
          }

          false -> {
            if ((it.type.resolve().declaration as KSClassDeclaration).isAnnotationPresent(Domain::class)) {
              PropertySpec.builder(it.simpleName.getShortName(), UUID::class.asClassName().copy(nullable = true))
                .apply {
                  initializer(it.simpleName.getShortName())
                }.build()
            } else {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus(".Update")
              val cn = ClassName(API_MODELS_PACKAGE_NAME, t).copy(nullable = true)
              PropertySpec.builder(n, cn).apply {
                initializer(n)
              }.build()
            }
          }
        }
        addProperty(prop)
      }
    }.build())
  }

  private fun TypeSpec.Builder.updatePrimaryConstructor(properties: Sequence<KSPropertyDeclaration>) {
    primaryConstructor(FunSpec.constructorBuilder().apply {
      properties.forEach {
        val param = when (it.type.isSupportedScalar()) {
          true -> {
            ParameterSpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).build()
          }

          false -> {
            if ((it.type.resolve().declaration as KSClassDeclaration).isAnnotationPresent(Domain::class)) {
              ParameterSpec.builder(it.simpleName.getShortName(), UUID::class.asClassName().copy(nullable = true))
                .apply {
                  addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
                    addMember("with = %T::class", Serializers.Uuid::class)
                  }.build())
                }.build()
            } else {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus(".Update")
              val cn = ClassName(API_MODELS_PACKAGE_NAME, t).copy(nullable = true)
              ParameterSpec.builder(n, cn).build()
            }
          }
        }
        addParameter(param)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addResponseModel(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
      .filterNot { it.isAnnotationPresent(Sensitive::class) }
      .filterNot { it.isAnnotationPresent(Relation::class) }
    addType(TypeSpec.classBuilder("Response").apply {
      addOriginatingKSFile(containingFile)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addModifiers(KModifier.DATA)
      addSuperinterface(IOResponse::class.asTypeName())
      responsePrimaryConstructor(properties)
      properties.forEach {
        val prop = when (it.type.isSupportedScalar()) {
          true -> it.toProperty()
          false -> {
            val n = it.simpleName.getShortName()
            val domain =
              (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).firstOrNull()
            if (domain != null) {
              val responseClass = ClassName(API_MODELS_PACKAGE_NAME, domain.name.plus("IOModels.Response"))
              PropertySpec.builder(n, responseClass).apply {
                initializer(n)
              }.build()
            } else {
              val t = it.type.resolve().toClassName().simpleName.plus(".Response")
              val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
              PropertySpec.builder(n, cn).apply {
                initializer(n)
              }.build()
            }
          }
        }
        addProperty(prop)
      }
      addType(TypeSpec.companionObjectBuilder().apply {
        addSuperinterface(
          ConvertFrom::class.asTypeName()
            .parameterizedBy(charter.classDeclaration.toClassName(), charter.apiResponseClass)
        )
        addFunction(FunSpec.builder("from").apply {
          addModifiers(KModifier.OVERRIDE)
          addParameter("input", charter.classDeclaration.toClassName())
          returns(charter.apiResponseClass)
          addCodeBlock {
            addObjectInstantiation(charter.apiResponseClass, returnInstance = true) {
              addConverterProperties(properties)
            }
          }
        }.build())
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.responsePrimaryConstructor(
    properties: Sequence<KSPropertyDeclaration>
  ) {
    primaryConstructor(FunSpec.constructorBuilder().apply {
      properties.forEach {
        val param = when (it.type.isSupportedScalar()) {
          true -> it.toParameter()
          false -> {
            val domain =
              (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).firstOrNull()
            if (domain != null) {
              val responseClass = ClassName(API_MODELS_PACKAGE_NAME, domain.name.plus("IOModels.Response"))
              ParameterSpec.builder(it.simpleName.getShortName(), responseClass).build()
            } else {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus(".Response")
              val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
              ParameterSpec.builder(n, cn).build()
            }
          }
        }
        addParameter(param)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addConverterProperties(properties: Sequence<KSPropertyDeclaration>) {
    properties.filterNot { it.isAnnotationPresent(Sensitive::class) }.forEach {
      when (it.type.isSupportedScalar()) {
        true -> addStatement("${it.simpleName.getShortName()} = input.${it.simpleName.getShortName()},")
        false -> {
          val n = it.simpleName.getShortName()
          val domain =
            (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).firstOrNull()
          if (domain != null) {
            val responseClass = ClassName(API_MODELS_PACKAGE_NAME, domain.name.plus("IOModels.Response"))
            addStatement("$n = ${responseClass.simpleName}.from(input.${n}),")
          } else {
            val t = it.type.resolve().toClassName().simpleName.plus(".Response")
            val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
            addStatement("$n = ${cn.simpleName}.from(input.${n}),")
          }
        }
      }
    }
  }
}
