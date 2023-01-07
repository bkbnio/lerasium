package io.bkbn.lerasium.api.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
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
import io.bkbn.lerasium.core.Sensitive
import io.bkbn.lerasium.core.converter.ConvertFrom
import io.bkbn.lerasium.core.model.IORequest
import io.bkbn.lerasium.core.model.IOResponse
import io.bkbn.lerasium.core.serialization.Serializers
import io.bkbn.lerasium.utils.KSVisitorWithData
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_MODELS_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.NestedLerasiumCharter
import kotlinx.serialization.Serializable

@OptIn(KspExperimental::class)
class NestedModelVisitor(private val typeBuilder: TypeSpec.Builder, private val logger: KSPLogger) :
  KSVisitorWithData<NestedModelVisitor.Data>() {

  data class Data(
    val parentCharter: LerasiumCharter,
    val visitedModels: Set<KSTypeReference> = setOf(),
  )

  override fun visitTypeReference(typeReference: KSTypeReference, data: Data) {
    val classDeclaration = typeReference.resolve().declaration as KSClassDeclaration

    val charter = NestedLerasiumCharter(
      parentCharter = data.parentCharter,
      classDeclaration = classDeclaration,
    )

    val newData = Data(
      parentCharter = charter,
      visitedModels = data.visitedModels.plus(typeReference),
    )

    typeBuilder.addChildObject(charter, newData)
  }

  private fun TypeSpec.Builder.addChildObject(charter: NestedLerasiumCharter, data: Data) {
    addType(TypeSpec.objectBuilder(charter.classDeclaration.simpleName.getShortName()).apply {
      addCreateRequest(charter)
      addUpdateRequest(charter)
      addResponse(charter)

      val childModelVisitor = NestedModelVisitor(this, logger)

      charter.classDeclaration.getAllProperties().toList()
        .filterNot { it.type.isSupportedScalar() }
        .filterNot { data.visitedModels.contains(it.type) }
        .forEach { childModelVisitor.visitTypeReference(it.type, data) }
    }.build())
  }

  private fun TypeSpec.Builder.addCreateRequest(charter: NestedLerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
    addType(TypeSpec.classBuilder("Create").apply {
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> it.toParameter()
            false -> {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus(".Create")
              val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
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
            val t = it.type.resolve().toClassName().simpleName.plus(".Create")
            val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
            PropertySpec.builder(n, cn).apply {
              initializer(n)
            }.build()
          }
        }
        addProperty(prop)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateRequest(charter: NestedLerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
    addType(TypeSpec.classBuilder("Update").apply {
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(IORequest.Update::class)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> {
              ParameterSpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).build()
            }

            false -> {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus(".Update")
              val cn = ClassName(API_MODELS_PACKAGE_NAME, t).copy(nullable = true)
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
              if (it.type.resolve().toClassName().simpleName == "UUID") {
                addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
                  addMember("with = %T::class", Serializers.Uuid::class)
                }.build())
              }
              initializer(it.simpleName.getShortName())
            }.build()
          }

          false -> {
            val n = it.simpleName.getShortName()
            val t = it.type.resolve().toClassName().simpleName.plus(".Update")
            val cn = ClassName(API_MODELS_PACKAGE_NAME, t).copy(nullable = true)
            PropertySpec.builder(n, cn).apply {
              initializer(n)
            }.build()
          }
        }
        addProperty(prop)
      }
    }.build())
  }

  @OptIn(KspExperimental::class)
  private fun TypeSpec.Builder.addResponse(charter: NestedLerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
      .filterNot { it.isAnnotationPresent(Sensitive::class) }
    addType(TypeSpec.classBuilder("Response").apply {
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(IOResponse::class)
      createFunctionPrimaryConstructor(properties)
      createFunctionProperties(properties)
      addType(TypeSpec.companionObjectBuilder().apply {
        addSuperinterface(
          ConvertFrom::class.asTypeName()
            .parameterizedBy(
              charter.classDeclaration.toClassName(),
              charter.apiResponseClass
            )
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

  private fun TypeSpec.Builder.createFunctionPrimaryConstructor(properties: Sequence<KSPropertyDeclaration>) {
    primaryConstructor(FunSpec.constructorBuilder().apply {
      properties.forEach {
        val param = when (it.type.isSupportedScalar()) {
          true -> it.toParameter()
          false -> {
            val n = it.simpleName.getShortName()
            val t = it.type.resolve().toClassName().simpleName.plus(".Response")
            val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
            ParameterSpec.builder(n, cn).build()
          }
        }
        addParameter(param)
      }
    }.build())
  }

  @Suppress("NestedBlockDepth")
  private fun TypeSpec.Builder.createFunctionProperties(properties: Sequence<KSPropertyDeclaration>) {
    properties.forEach {
      val prop = when (it.type.isSupportedScalar()) {
        true -> it.toProperty()
        false -> {
          val n = it.simpleName.getShortName()
          val t = it.type.resolve().toClassName().simpleName.plus(".Response")
          val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
          PropertySpec.builder(n, cn).apply {
            initializer(n)
            if (it.type.resolve().toClassName().simpleName == "UUID") {
              addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
                addMember("with = %T::class", Serializers.Uuid::class)
              }.build())
            }
          }.build()
        }
      }
      addProperty(prop)
    }
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
            val responseClass = ClassName(API_MODELS_PACKAGE_NAME, domain.name.plus("Models.Response"))
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
