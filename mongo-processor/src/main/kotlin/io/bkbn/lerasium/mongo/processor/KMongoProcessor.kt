package io.bkbn.lerasium.mongo.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.lerasium.mongo.Document
import io.bkbn.lerasium.mongo.processor.visitor.DaoVisitor
import io.bkbn.lerasium.mongo.processor.visitor.RepositoryVisitor
import io.bkbn.lerasium.mongo.processor.visitor.RootDocumentVisitor
import io.bkbn.lerasium.utils.KotlinPoetUtils.DOCUMENT_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.REPOSITORY_PACKAGE_NAME
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain

class KMongoProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  init {
    logger.info(options.toString())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation(Document::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()

    if (!symbols.iterator().hasNext()) return emptyList()

    symbols.forEach {
      val domain = it.getDomain()
      val fb = FileSpec.builder(DOCUMENT_PACKAGE_NAME, domain.name.plus("Document"))
      it.accept(RootDocumentVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    symbols.forEach {
      val domain = it.getDomain()
      val fb = FileSpec.builder(REPOSITORY_PACKAGE_NAME, domain.name.plus("Dao"))
      it.accept(DaoVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    symbols.forEach {
      val domain = it.getDomain()
      val fb = FileSpec.builder(REPOSITORY_PACKAGE_NAME, domain.name.plus("Repository"))
      it.accept(RepositoryVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    return symbols.filterNot { it.validate() }.toList()
  }
}
