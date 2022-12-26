package io.bkbn.lerasium.api.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.api.processor.visitor.ControllerVisitor
import io.bkbn.lerasium.api.processor.visitor.DocumentationVisitor
import io.bkbn.lerasium.api.processor.visitor.ServiceVisitor
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_CONTROLLER_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_DOCS_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_SERVICE_PACKAGE_NAME
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain

class KtorProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  init {
    logger.info(options.toString())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation(Api::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()

    if (!symbols.iterator().hasNext()) return emptyList()

    symbols.forEach { it.writeControllerFile() }
    symbols.forEach { it.writeDocFile() }
    symbols.forEach { it.writeServiceFile() }

    return symbols.filterNot { it.validate() }.toList()
  }

  private fun KSClassDeclaration.writeControllerFile() {
    val domain = this.findParentDomain()
    val fb = FileSpec.builder(API_CONTROLLER_PACKAGE_NAME, domain.name.plus("Controller"))
    this.accept(ControllerVisitor(fb, logger), Unit)
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }

  private fun KSClassDeclaration.writeDocFile() {
    val domain = this.findParentDomain()
    val fb = FileSpec.builder(API_DOCS_PACKAGE_NAME, domain.name.plus("Documentation"))
    this.accept(DocumentationVisitor(fb, logger), Unit)
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }

  private fun KSClassDeclaration.writeServiceFile() {
    val domain = this.findParentDomain()
    val fb = FileSpec.builder(API_SERVICE_PACKAGE_NAME, domain.name.plus("Service"))
    this.accept(ServiceVisitor(fb, logger), Unit)
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }
}
