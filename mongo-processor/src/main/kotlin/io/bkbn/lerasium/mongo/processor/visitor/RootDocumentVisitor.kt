package io.bkbn.lerasium.mongo.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
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
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.converter.ConvertTo
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.LerasiumUtils.isCollection
import io.bkbn.lerasium.utils.LerasiumUtils.isDomain
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@OptIn(KspExperimental::class)
class RootDocumentVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  private lateinit var containingFile: KSFile

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Domain", classDeclaration)
      return
    }

    containingFile = classDeclaration.containingFile!!

    val domain = classDeclaration.getDomain()
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addDocument(charter)
  }

  private fun FileSpec.Builder.addDocument(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
      .filterNot { it.type.isDomain() }
      .filterNot { it.simpleName.getShortName() == "id" }
    addType(TypeSpec.classBuilder(charter.documentClass).apply {
      addOriginatingKSFile(charter.classDeclaration.containingFile!!)
      addSuperinterface(ConvertTo::class.asClassName().parameterizedBy(charter.domainClass))
      addAnnotation(Serializable::class)
      addModifiers(KModifier.DATA)
      documentPrimaryConstructor(charter, properties)
      documentProperties(charter, properties)
      addDomainConverter(charter)

      val nestedDocumentVisitor = NestedDocumentVisitor(this, logger)
      charter.classDeclaration.getAllProperties()
        .filterNot { it.type.isSupportedScalar() }
        .filterNot { it.type.isCollection() }
        .filterNot { (it.type.resolve().declaration as KSClassDeclaration).isAnnotationPresent(Domain::class) }
        .forEach {
          nestedDocumentVisitor.visitTypeReference(
            it.type, NestedDocumentVisitor.Data(parentCharter = charter)
          )
        }
    }.build())
  }

  private fun TypeSpec.Builder.documentPrimaryConstructor(
    charter: LerasiumCharter,
    properties: Sequence<KSPropertyDeclaration>
  ) {
    primaryConstructor(FunSpec.constructorBuilder().apply {
      addParameter(ParameterSpec.builder("id", UUID::class).apply {
        addAnnotation(Contextual::class)
        addAnnotation(AnnotationSpec.builder(SerialName::class).apply {
          addMember("%S", "_id")
        }.build())
      }.build())
      properties.forEach {
        val param = when (it.type.isSupportedScalar()) {
          true -> it.toParameter()
          false -> {
            val n = it.simpleName.getShortName()
            val t = it.type.resolve().toClassName().simpleName.plus("Document")
            val p = charter.documentClass.canonicalName
            val cn = ClassName(p, t)
            ParameterSpec.builder(n, cn).build()
          }
        }
        addParameter(param)
      }
      addParameter(ParameterSpec("createdAt", LocalDateTime::class.asTypeName()))
      addParameter(ParameterSpec("updatedAt", LocalDateTime::class.asTypeName()))
    }.build())
  }

  private fun TypeSpec.Builder.documentProperties(
    charter: LerasiumCharter,
    properties: Sequence<KSPropertyDeclaration>,
  ) {
    addProperty(PropertySpec.builder("id", UUID::class.asTypeName()).apply {
      initializer("id")
    }.build())
    properties.forEach {
      val prop = when (it.type.isSupportedScalar()) {
        true -> it.toProperty(isMutable = true)
        false -> {
          val n = it.simpleName.getShortName()
          val t = it.type.resolve().toClassName().simpleName.plus("Document")
          val p = charter.documentClass.canonicalName
          val cn = ClassName(p, t)
          PropertySpec.builder(n, cn).apply {
            mutable()
            initializer(n)
          }.build()
        }
      }
      addProperty(prop)
    }
    addProperty(PropertySpec.builder("createdAt", LocalDateTime::class.asTypeName()).apply {
      initializer("createdAt")
    }.build())
    addProperty(PropertySpec.builder("updatedAt", LocalDateTime::class.asTypeName()).apply {
      mutable()
      initializer("updatedAt")
    }.build())
  }

  private fun TypeSpec.Builder.addDomainConverter(charter: LerasiumCharter) {
    val scalarProps = charter.classDeclaration.getAllProperties()
      .filter { it.type.isSupportedScalar() }
      .filterNot { it.simpleName.getShortName() == "id" }
    val nestedProps = charter.classDeclaration.getAllProperties()
      .filterNot { it in scalarProps }
      .filterNot { it.simpleName.getShortName() == "id" }
    addFunction(FunSpec.builder("to").apply {
      returns(charter.domainClass)
      addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
      addCodeBlock {
        addObjectInstantiation(charter.domainClass, returnInstance = true) {
          addStatement("id = id,")
          scalarProps.forEach {
            addStatement("%L = %L,", it.simpleName.getShortName(), it.simpleName.getShortName())
          }
          nestedProps.forEach {
            addStatement("%L = %L.to(),", it.simpleName.getShortName(), it.simpleName.getShortName())
          }
        }
      }
    }.build())
  }
}
