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

    symbols.forEach {
      val domain = it.findParentDomain()
      val fb = FileSpec.builder(BASE_API_PACKAGE_NAME, domain.name.plus("Api"))
      it.accept(ApiVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    symbols.forEach {
      val domain = it.findParentDomain()
      val fb = FileSpec.builder(BASE_API_PACKAGE_NAME, domain.name.plus("ToC"))
      it.accept(TocVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    symbols.forEach {
      val domain = it.findParentDomain()
      val fb = FileSpec.builder(BASE_API_PACKAGE_NAME, domain.name.plus("Queries"))
      it.accept(QueryModelVisitor(fb, logger), Unit)
      val fs = fb.build()
      if (fs.members.isNotEmpty()) {
        fs.writeTo(codeGenerator, false)
      }
    }

    return symbols.filterNot { it.validate() }.toList()
  }
}
