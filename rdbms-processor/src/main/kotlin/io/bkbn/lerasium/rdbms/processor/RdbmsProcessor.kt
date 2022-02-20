package io.bkbn.lerasium.rdbms.processor

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
import io.bkbn.lerasium.rdbms.Table
import io.bkbn.lerasium.utils.KotlinPoetUtils.BASE_ENTITY_PACKAGE_NAME
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
class RdbmsProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  init {
    logger.info(options.toString())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation(Table::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()

    if (!symbols.iterator().hasNext()) return emptyList()

    symbols.forEach {
      val domain = it.findParentDomain()
      val fb = FileSpec.builder(BASE_ENTITY_PACKAGE_NAME, domain.name.plus("Table"))
      it.accept(TableVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    symbols.forEach {
      val domain = it.findParentDomain()
      val fb = FileSpec.builder(BASE_ENTITY_PACKAGE_NAME, domain.name.plus("Dao"))
      it.accept(DaoVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    return symbols.filterNot { it.validate() }.toList()
  }
}
