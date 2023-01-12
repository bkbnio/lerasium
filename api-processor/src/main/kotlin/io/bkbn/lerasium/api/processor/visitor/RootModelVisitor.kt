package io.bkbn.lerasium.api.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
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
import io.bkbn.lerasium.core.converter.ConvertFrom
import io.bkbn.lerasium.core.model.IORequest
import io.bkbn.lerasium.core.model.IOResponse
import io.bkbn.lerasium.core.serialization.Serializers
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_MODELS_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.collectProperties
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.PropertyWrapper
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

      charter.classDeclaration.collectProperties().nested.forEach {
        nestedModelVisitor.visitTypeReference(
          it.type, NestedModelVisitor.Data(parentCharter = charter)
        )
      }
    }.build())
  }

  private fun TypeSpec.Builder.addCreateRequestModel(charter: LerasiumCharter) {
    val props = charter.classDeclaration.collectProperties()
    addType(TypeSpec.classBuilder("Create").apply {
      addOriginatingKSFile(containingFile)
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(IORequest.Create::class)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        props.scalars.forEach { addParameter(it.toParameter()) }
        props.domain.forEach {
          val n = it.simpleName.getShortName()
          addParameter(
            ParameterSpec.builder(n, UUID::class).apply {
              addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
                addMember("with = %T::class", Serializers.Uuid::class)
              }.build())
            }.build()
          )
        }
        props.nested.forEach {
          val n = it.simpleName.getShortName()
          val t = it.type.resolve().toClassName().simpleName.plus(".Create")
          val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
          addParameter(ParameterSpec.builder(n, cn).build())
        }
        props.enums.forEach { addParameter(it.simpleName.getShortName(), it.type.toTypeName()) }
      }.build())

      props.scalars.forEach { addProperty(it.toProperty()) }
      props.domain.forEach {
        val n = it.simpleName.getShortName()
        addProperty(PropertySpec.builder(n, UUID::class).apply {
          initializer(n)
        }.build())
      }
      props.nested.forEach {
        val n = it.simpleName.getShortName()
        val t = it.type.resolve().toClassName().simpleName.plus(".Create")
        val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
        addProperty(PropertySpec.builder(n, cn).apply {
          initializer(n)
        }.build())
      }
      props.enums.forEach { prop ->
        addProperty(PropertySpec.builder(prop.simpleName.getShortName(), prop.type.toTypeName()).apply {
          initializer(prop.simpleName.getShortName())
        }.build())
      }
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateRequestModel(charter: LerasiumCharter) {
    val props = charter.classDeclaration.collectProperties()
    addType(TypeSpec.classBuilder("Update").apply {
      addOriginatingKSFile(containingFile)
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addSuperinterface(IORequest.Update::class)
      updatePrimaryConstructor(props)
      props.scalars.forEach {
        addProperty(
          PropertySpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).apply {
            if (it.type.resolve().toClassName().simpleName == "UUID") {
              addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
                addMember("with = %T::class", Serializers.Uuid::class)
              }.build())
            }
            initializer(it.simpleName.getShortName())
          }.build()
        )
      }
      props.domain.forEach {
        addProperty(
          PropertySpec.builder(it.simpleName.getShortName(), UUID::class.asClassName().copy(nullable = true))
            .apply {
              initializer(it.simpleName.getShortName())
            }.build()
        )
      }
      props.nested.forEach {
        val n = it.simpleName.getShortName()
        val t = it.type.resolve().toClassName().simpleName.plus(".Update")
        val cn = ClassName(API_MODELS_PACKAGE_NAME, t).copy(nullable = true)
        addProperty(PropertySpec.builder(n, cn).apply {
          initializer(n)
        }.build())
      }
      props.enums.forEach {
        addProperty(
          PropertySpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).apply {
            initializer(it.simpleName.getShortName())
          }.build()
        )
      }
    }.build())
  }

  private fun TypeSpec.Builder.updatePrimaryConstructor(propWrapper: PropertyWrapper) {
    primaryConstructor(FunSpec.constructorBuilder().apply {
      propWrapper.scalars.forEach {
        addParameter(
          ParameterSpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).build()
        )
      }
      propWrapper.domain.forEach {
        addParameter(
          ParameterSpec.builder(it.simpleName.getShortName(), UUID::class.asClassName().copy(nullable = true))
            .apply {
              addAnnotation(AnnotationSpec.builder(Serializable::class).apply {
                addMember("with = %T::class", Serializers.Uuid::class)
              }.build())
            }.build()
        )
      }
      propWrapper.nested.forEach {
        val n = it.simpleName.getShortName()
        val t = it.type.resolve().toClassName().simpleName.plus(".Update")
        val cn = ClassName(API_MODELS_PACKAGE_NAME, t).copy(nullable = true)
        addParameter(ParameterSpec.builder(n, cn).build())
      }
      propWrapper.enums.forEach {
        addParameter(
          ParameterSpec.builder(it.simpleName.getShortName(), it.type.toTypeName().copy(nullable = true)).build()
        )
      }
    }.build())
  }

  private fun TypeSpec.Builder.addResponseModel(charter: LerasiumCharter) {
    val props = charter.classDeclaration.collectProperties().filterSensitive()
    addType(TypeSpec.classBuilder("Response").apply {
      addOriginatingKSFile(containingFile)
      addAnnotation(AnnotationSpec.builder(Serializable::class).build())
      addModifiers(KModifier.DATA)
      addSuperinterface(IOResponse::class.asTypeName())
      responsePrimaryConstructor(props)
      props.scalars.forEach { addProperty(it.toProperty()) }
      props.domain.forEach {
        val n = it.simpleName.getShortName()
        val domain = (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).first()
        val responseClass = ClassName(API_MODELS_PACKAGE_NAME, domain.name.plus("Models.Response"))
        addProperty(PropertySpec.builder(n, responseClass).apply {
          initializer(n)
        }.build())
      }
      props.nested.forEach {
        val n = it.simpleName.getShortName()
        val t = it.type.resolve().toClassName().simpleName.plus(".Response")
        val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
        addProperty(PropertySpec.builder(n, cn).apply {
          initializer(n)
        }.build())
      }
      props.enums.forEach {
        addProperty(
          PropertySpec.builder(it.simpleName.getShortName(), it.type.toTypeName()).apply {
            initializer(it.simpleName.getShortName())
          }.build()
        )
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
              addConverterProperties(props)
            }
          }
        }.build())
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.responsePrimaryConstructor(
    propWrapper: PropertyWrapper
  ) {
    primaryConstructor(FunSpec.constructorBuilder().apply {
      propWrapper.scalars.forEach { addParameter(it.toParameter()) }
      propWrapper.domain.forEach {
        val n = it.simpleName.getShortName()
        val domain = (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).first()
        val responseClass = ClassName(API_MODELS_PACKAGE_NAME, domain.name.plus("Models.Response"))
        addParameter(ParameterSpec.builder(n, responseClass).build())
      }
      propWrapper.nested.forEach {
        val n = it.simpleName.getShortName()
        val t = it.type.resolve().toClassName().simpleName.plus(".Response")
        val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
        addParameter(ParameterSpec.builder(n, cn).build())
      }
      propWrapper.enums.forEach {
        addParameter(it.toParameter())
      }
    }.build())
  }

  private fun CodeBlock.Builder.addConverterProperties(propWrapper: PropertyWrapper) {
    val filteredProps = propWrapper.filterSensitive()
    filteredProps.scalars.forEach {
      addStatement("${it.simpleName.getShortName()} = input.${it.simpleName.getShortName()},")
    }
    filteredProps.domain.forEach {
      val n = it.simpleName.getShortName()
      val domain = (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).first()
      val responseClass = ClassName(API_MODELS_PACKAGE_NAME, domain.name.plus("Models.Response"))
      addStatement("$n = ${responseClass.simpleName}.from(input.${n}),")
    }
    filteredProps.nested.forEach {
      val n = it.simpleName.getShortName()
      val t = it.type.resolve().toClassName().simpleName.plus(".Response")
      val cn = ClassName(API_MODELS_PACKAGE_NAME, t)
      addStatement("$n = ${cn.simpleName}.from(input.${n}),")
    }
    filteredProps.enums.forEach {
      addStatement("${it.simpleName.getShortName()} = input.${it.simpleName.getShortName()},")
    }
  }
}
