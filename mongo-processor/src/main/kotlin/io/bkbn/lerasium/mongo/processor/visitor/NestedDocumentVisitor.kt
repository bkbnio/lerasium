package io.bkbn.lerasium.mongo.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.converter.ConvertTo
import io.bkbn.lerasium.utils.KSVisitorWithData
import io.bkbn.lerasium.utils.KotlinPoetUtils.DOCUMENT_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.isDomain
import io.bkbn.lerasium.utils.NestedLerasiumCharter
import kotlinx.serialization.Serializable

@OptIn(KspExperimental::class)
class NestedDocumentVisitor(
  private val typeBuilder: TypeSpec.Builder,
  private val logger: KSPLogger
) : KSVisitorWithData<NestedDocumentVisitor.Data>() {

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

    typeBuilder.addChildDocument(charter, newData)
  }

  private fun TypeSpec.Builder.addChildDocument(charter: NestedLerasiumCharter, data: Data) {
    val properties = charter.classDeclaration.getAllProperties()
      .filterNot { it.isAnnotationPresent(Relation::class) }
      .filterNot { it.type.isDomain() }
    addType(TypeSpec.classBuilder(charter.classDeclaration.simpleName.getShortName().plus("Document")).apply {
      addAnnotation(Serializable::class)
      addModifiers(KModifier.DATA)
      addSuperinterface(ConvertTo::class.asClassName().parameterizedBy(charter.domainClass))
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> it.toParameter()
            false -> {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus("Document")
              val p =  charter.documentClass.canonicalName
              val cn = ClassName(p, t)
              ParameterSpec.builder(n, cn).build()
            }
          }
          addParameter(param)
        }
      }.build())
      properties.forEach {
        val prop = when (it.type.isSupportedScalar()) {
          true -> it.toProperty(isMutable = true)
          false -> {
            val n = it.simpleName.getShortName()
            val t = it.type.resolve().toClassName().simpleName.plus("Document")
            val p =  charter.documentClass.canonicalName
            val cn = ClassName(p, t)
            PropertySpec.builder(n, cn).apply {
              mutable()
              initializer(n)
            }.build()
          }
        }
        addProperty(prop)
      }

      addDomainConverter(charter)

      val childDocumentVisitor = NestedDocumentVisitor(this, logger)
      charter.classDeclaration.getAllProperties()
        .filterNot { it.type.isSupportedScalar() }
        .filterNot { data.visitedModels.contains(it.type) }
        .forEach { childDocumentVisitor.visitTypeReference(it.type, data) }
    }.build())
  }

  private fun TypeSpec.Builder.addDomainConverter(charter: LerasiumCharter) {
    val scalarProps = charter.classDeclaration.getAllProperties()
      .filter { it.type.isSupportedScalar() }
    val nestedProps = charter.classDeclaration.getAllProperties()
      .filterNot { it in scalarProps }
    addFunction(FunSpec.builder("to").apply {
      returns(charter.domainClass)
      addModifiers(KModifier.OVERRIDE)
      addCodeBlock {
        addControlFlow("return object : %T", charter.domainClass) {
          scalarProps.forEach {
            addStatement("override val %L: %T", it.simpleName.getShortName(), it.type.toTypeName())
            indent()
            addStatement("get() = this@%L.%L", charter.documentClass.simpleName, it.simpleName.getShortName())
            unindent()
          }
          nestedProps.forEach {
            addStatement("override val %L: %T", it.simpleName.getShortName(), it.type.toTypeName())
            indent()
            addStatement("get() = this@%L.%L.to()", charter.documentClass.simpleName, it.simpleName.getShortName())
            unindent()
          }
        }
      }
    }.build())
  }
}
