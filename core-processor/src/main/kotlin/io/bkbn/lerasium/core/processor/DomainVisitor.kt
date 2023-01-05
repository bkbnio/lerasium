package io.bkbn.lerasium.core.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain

@OptIn(KspExperimental::class)
class DomainVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {
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
    val properties = charter.classDeclaration.getAllProperties()
    addType(TypeSpec.classBuilder(charter.domain.name).apply {
      addAliasedImport(charter.classDeclaration.toClassName(), "Domain")
      addSuperinterface(charter.classDeclaration.toClassName())
      addModifiers(KModifier.DATA)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> it.toParameter()
            false -> {
              val n = it.simpleName.getShortName()
              val domain =
                (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).firstOrNull()
              if (domain != null) {
                ParameterSpec.builder(n, it.type.toTypeName()).build()
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
      }.build())
      properties.forEach {
        val prop = when (it.type.isSupportedScalar()) {
          true -> it.toProperty(isOverride = true)
          false -> {
            val n = it.simpleName.getShortName()
            val domain =
              (it.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).firstOrNull()
            if (domain != null) {
              PropertySpec.builder(n, it.type.toTypeName()).apply {
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
    }.build())
  }
}
