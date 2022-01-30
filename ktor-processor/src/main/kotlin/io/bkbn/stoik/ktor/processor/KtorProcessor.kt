package io.bkbn.stoik.ktor.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.stoik.ktor.Api
import io.bkbn.stoik.utils.StoikUtils.findParentDomain

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
class KtorProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  companion object {
    private const val BASE_PACKAGE_NAME = "io.bkbn.stoik.generated.api"
  }

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
      val fb = FileSpec.builder(BASE_PACKAGE_NAME, domain.name.plus("Api"))
      it.accept(ApiVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    return symbols.filterNot { it.validate() }.toList()
  }
}
