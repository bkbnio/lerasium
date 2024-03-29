package io.bkbn.lerasium.core.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.utils.KotlinPoetUtils
import io.bkbn.lerasium.utils.KotlinPoetUtils.DOMAIN_PACKAGE_NAME
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.LerasiumUtils.isActor

class DomainProcessor(
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
      val fb = FileSpec.builder(DOMAIN_PACKAGE_NAME, domain.name.plus("Domain"))
      it.accept(RootDomainVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    writePolicyConfig(symbols)

    return symbols.filterNot { it.validate() }.toList()
  }

  private fun writePolicyConfig(symbols: Sequence<KSClassDeclaration>) {
    val fb = FileSpec.builder(KotlinPoetUtils.POLICY_PACKAGE_NAME, "PolicyConfig")
    fb.addPolicyConfig(symbols)
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }

  private fun FileSpec.Builder.addPolicyConfig(symbols: Sequence<KSClassDeclaration>) {
    addType(TypeSpec.enumBuilder("Actor").apply {
      symbols.filter { it.isActor() }.forEach { actor ->
        addEnumConstant(actor.simpleName.asString().uppercase())
      }
    }.build())
  }
}
