package io.bkbn.lerasium.api.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.kompendium.oas.serialization.KompendiumSerializersModule
import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.api.processor.Members.authenticationMember
import io.bkbn.lerasium.api.processor.Members.contentNegotiationMember
import io.bkbn.lerasium.api.processor.Members.installMember
import io.bkbn.lerasium.api.processor.Members.kotlinxJsonMember
import io.bkbn.lerasium.api.processor.Members.ktorJsonMember
import io.bkbn.lerasium.api.processor.visitor.ConfigVisitor
import io.bkbn.lerasium.api.processor.visitor.ControllerVisitor
import io.bkbn.lerasium.api.processor.visitor.DocumentationVisitor
import io.bkbn.lerasium.api.processor.visitor.RootModelVisitor
import io.bkbn.lerasium.api.processor.visitor.ServiceVisitor
import io.bkbn.lerasium.core.auth.Actor
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_CONFIG_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_CONTROLLER_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_DOCS_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_MODELS_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.API_SERVICE_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.LerasiumUtils.findParent
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.ktor.server.application.Application
import java.util.Locale

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
    symbols.forEach { it.writeModelFile() }

    symbols.writeConfigFile()

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

  private fun KSClassDeclaration.writeModelFile() {
    val domain = this.findParentDomain()
    val fb = FileSpec.builder(API_MODELS_PACKAGE_NAME, domain.name.plus("Models"))
    this.accept(RootModelVisitor(fb, logger), Unit)
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }

  @OptIn(KspExperimental::class)
  private fun Sequence<KSClassDeclaration>.writeConfigFile() {
    val fb = FileSpec.builder(API_CONFIG_PACKAGE_NAME, "ApiConfig")
    val actors = this.map { it.findParent() }
      .filter { it.isAnnotationPresent(Actor::class) }
      .map { it.getDomain().name }
    fb.addConfigEntrypoint(actors.toList())
    this.forEach { it.accept(ConfigVisitor(fb, logger), Unit) }
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }

  private fun FileSpec.Builder.addConfigEntrypoint(actors: List<String>) {
    addFunction(FunSpec.builder("lerasiumConfig").apply {
      receiver(Application::class)
      addCodeBlock {
        addControlFlow("val json = %M", kotlinxJsonMember) {
          addStatement("serializersModule = %T.module", KompendiumSerializersModule::class)
          addStatement("prettyPrint = true")
          addStatement("encodeDefaults = true")
          addStatement("explicitNulls = false")
        }
        addControlFlow("%M(%M)", installMember, contentNegotiationMember) {
          addStatement("%M(json)", ktorJsonMember)
        }
        if (actors.isNotEmpty()) {
          addControlFlow("%M", authenticationMember) {
            actors.forEach {
              addStatement("${it}AuthConfig()".replaceFirstChar { c -> c.lowercase(Locale.getDefault()) })
            }
          }
        }
      }
    }.build())
  }
}
