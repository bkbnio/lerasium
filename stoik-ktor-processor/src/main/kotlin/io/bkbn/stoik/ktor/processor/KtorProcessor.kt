package io.bkbn.stoik.ktor.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import io.bkbn.stoik.ktor.core.Api
import io.bkbn.stoik.ktor.processor.util.RouteUtils.addControlFlow
import io.bkbn.stoik.ktor.processor.util.StringHelpers.snakeToLowerCamelCase
import io.bkbn.stoik.ktor.processor.util.StringHelpers.snakeToUpperCamelCase
import io.ktor.routing.Route
import java.io.OutputStream

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
      val file: OutputStream = codeGenerator.createNewFile(
        dependencies = Dependencies(false),
        packageName = "io.bkbn.stoik.generated",
        fileName = "Apis"
      )
      val fb = FileSpec.builder("io.bkbn.stoik.generated", "Hi")
      it.accept(Visitor(fb), Unit)
      val fs = fb.build()
      file.write(fs.toString().toByteArray())
    }

    return symbols.filterNot { it.validate() }.toList()
  }

  inner class Visitor(private val fileBuilder: FileSpec.Builder) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      if (classDeclaration.classKind != ClassKind.INTERFACE) {
        logger.error("Only an interface can be decorated with @Table", classDeclaration)
        return
      }

      val annotation: KSAnnotation = classDeclaration.annotations.first {
        it.shortName.asString() == Api::class.simpleName
      }

      val nameArgument: KSValueArgument = annotation.arguments
        .first { arg -> arg.name?.asString() == "name" }

      val apiName = nameArgument.value as String
      val controllerName = apiName.plus("Controller").decapitalize()
      val apiObjectName = apiName.plus("Api")

      val routeMember = MemberName("io.ktor.routing", "route")
      val getMember = MemberName("io.ktor.routing", "get")
      val callMember = MemberName("io.ktor.application", "call")
      val respondText = MemberName("io.ktor.response", "respondText")

      fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
        addFunction(FunSpec.builder(controllerName).apply {
          receiver(Route::class)
          addCode(buildCodeBlock {
            addControlFlow("%M(%S)", routeMember, "/") {
              addControlFlow("%M", getMember) {
                addControlFlow("%M.%M", callMember, respondText) {
                  addStatement("%S", "hi")
                }
              }
            }
          })
        }.build())
      }.build())
    }
  }
}
