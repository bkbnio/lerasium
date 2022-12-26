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
import io.bkbn.lerasium.utils.KotlinPoetUtils.BASE_API_PACKAGE_NAME
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

    symbols.forEach { it.writeApiFile() }

    symbols.forEach { it.writeDocFile() }

    return symbols.filterNot { it.validate() }.toList()
  }

  private fun KSClassDeclaration.writeApiFile() {
    val domain = this.findParentDomain()
    val fb = FileSpec.builder(BASE_API_PACKAGE_NAME, domain.name.plus("Api"))
    this.accept(ApiVisitor(fb, logger), Unit)
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }

  private fun KSClassDeclaration.writeDocFile() {
    val domain = this.findParentDomain()
    val fb = FileSpec.builder(BASE_API_PACKAGE_NAME, domain.name.plus("ApiDocs"))
    this.accept(DocumentationVisitor(fb, logger), Unit)
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }
}
