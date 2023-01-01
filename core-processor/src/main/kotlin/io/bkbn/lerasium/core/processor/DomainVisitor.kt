package io.bkbn.lerasium.core.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain

class DomainVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {
  private lateinit var containingFile: KSFile

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Domain", classDeclaration)
      return
    }

    containingFile = classDeclaration.containingFile!!

    val domain = classDeclaration.findParentDomain()
    val charter = LerasiumCharter(domain, classDeclaration)

    // fileBuilder.addIoModelObject(charter)
    fileBuilder.addDomainModels(charter)
  }

  private fun FileSpec.Builder.addDomainModels(charter: LerasiumCharter) {
    addType(TypeSpec.classBuilder(charter.domain.name.plus("Domain")).apply {

    }.build())
  }
}
