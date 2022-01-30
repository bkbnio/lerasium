package io.bkbn.stoik.core.processor

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
import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.utils.KotlinPoetUtils.BASE_MODEL_PACKAGE_NAME
import io.bkbn.stoik.utils.StoikUtils.getDomain

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
class ModelProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  init {
    logger.info(options.toString())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation(Domain::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()

    if (!symbols.iterator().hasNext()) return emptyList()

    symbols.forEach {
      val domain = it.getDomain()
      val fb = FileSpec.builder(BASE_MODEL_PACKAGE_NAME, domain.name.plus("Models"))
      it.accept(ModelVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    return symbols.filterNot { it.validate() }.toList()
  }
}
