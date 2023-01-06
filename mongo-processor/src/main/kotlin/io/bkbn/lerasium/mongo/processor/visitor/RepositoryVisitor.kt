package io.bkbn.lerasium.mongo.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain

class RepositoryVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {
  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.getDomain()
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addRepository(charter)
  }

  private fun FileSpec.Builder.addRepository(charter: LerasiumCharter) {
    addType(TypeSpec.objectBuilder(charter.domain.name.plus("Repository")).apply {
      addOriginatingKSFile(charter.classDeclaration.containingFile!!)
      addCreateFunction(charter)
      addReadFunction(charter)
      addUpdateFunction(charter)
      addDeleteFunction(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addCreateFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("create").apply {
      returns(charter.domainClass)
      addStatement("TODO()")
    }.build())
  }

  private fun TypeSpec.Builder.addReadFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("read").apply {
      returns(charter.domainClass)
      addStatement("TODO()")
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("update").apply {
      returns(charter.domainClass)
      addStatement("TODO()")
    }.build())
  }

  private fun TypeSpec.Builder.addDeleteFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("delete").apply {
      addStatement("TODO()")
    }.build())
  }
}
