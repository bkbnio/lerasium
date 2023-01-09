package io.bkbn.lerasium.permissions.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import io.bkbn.lerasium.core.Domain

class PermissionProcessor(
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

    return symbols.filterNot { it.validate() }.toList()
  }
}
