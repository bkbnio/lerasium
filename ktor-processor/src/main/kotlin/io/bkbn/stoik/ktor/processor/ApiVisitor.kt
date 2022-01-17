package io.bkbn.stoik.ktor.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.stoik.ktor.processor.util.RouteUtils.addControlFlow
import io.bkbn.stoik.utils.StoikUtils.findValidDomain
import io.ktor.routing.Route
import java.util.Locale

@OptIn(KotlinPoetKspPreview::class)
class ApiVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val apiName = classDeclaration.findValidDomain().name
    val controllerName = apiName.plus("Controller").replaceFirstChar { it.lowercase(Locale.getDefault()) }
    val apiObjectName = apiName.plus("Api")

    val routeMember = MemberName("io.ktor.routing", "route")
    val getMember = MemberName("io.ktor.routing", "get")
    val callMember = MemberName("io.ktor.application", "call")
    val respondText = MemberName("io.ktor.response", "respondText")

    fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
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
