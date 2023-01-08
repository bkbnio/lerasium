package io.bkbn.lerasium.core.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.utils.KSVisitorWithData
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.isDomain
import io.bkbn.lerasium.utils.NestedLerasiumCharter

@OptIn(KspExperimental::class)
class NestedDomainVisitor(
  private val typeBuilder: TypeSpec.Builder,
  private val logger: KSPLogger
) : KSVisitorWithData<NestedDomainVisitor.Data>() {

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

    typeBuilder.addChildDomain(charter, newData)
  }

  private fun TypeSpec.Builder.addChildDomain(charter: NestedLerasiumCharter, data: Data) {
    val properties = charter.classDeclaration.getAllProperties()
      .filterNot { it.isAnnotationPresent(Relation::class) }
      .filterNot { it.type.isDomain() }
    addType(TypeSpec.classBuilder(charter.classDeclaration.simpleName.getShortName()).apply {
      addModifiers(KModifier.DATA)
      addSuperinterface(charter.domainClass)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> it.toParameter()
            false -> {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName
              val p = charter.domainClass.canonicalName
              val cn = ClassName(p, t)
              ParameterSpec.builder(n, cn).build()
            }
          }
          addParameter(param)
        }
      }.build())
      properties.forEach {
        val prop = when (it.type.isSupportedScalar()) {
          true -> it.toProperty(isOverride = true, serializable = false)
          false -> {
            val n = it.simpleName.getShortName()
            val t = it.type.resolve().toClassName().simpleName
            val p = charter.domainClass.canonicalName
            val cn = ClassName(p, t)
            PropertySpec.builder(n, cn).apply {
              initializer(n)
              addModifiers(KModifier.OVERRIDE)
            }.build()
          }
        }
        addProperty(prop)
      }
      val childDomainVisitor = NestedDomainVisitor(this, logger)
      charter.classDeclaration.getAllProperties()
        .filterNot { it.type.isSupportedScalar() }
        .filterNot { data.visitedModels.contains(it.type) }
        .forEach { childDomainVisitor.visitTypeReference(it.type, data) }
    }.build())
  }
}
