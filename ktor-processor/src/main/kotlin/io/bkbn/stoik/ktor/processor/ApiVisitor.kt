@file:OptIn(KspExperimental::class)

package io.bkbn.stoik.ktor.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.stoik.utils.BaseName
import io.bkbn.stoik.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.stoik.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.stoik.utils.StoikUtils.findParentDomain
import io.bkbn.stoik.utils.toCreateRequestClass
import io.bkbn.stoik.utils.toUpdateRequestClass
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Routing
import java.util.Locale
import java.util.UUID

@OptIn(KotlinPoetKspPreview::class)
class ApiVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    val routeMember = MemberName("io.ktor.routing", "route")
    val getMember = MemberName("io.ktor.routing", "get")
    val postMember = MemberName("io.ktor.routing", "post")
    val putMember = MemberName("io.ktor.routing", "put")
    val deleteMember = MemberName("io.ktor.routing", "delete")
    val callMember = MemberName("io.ktor.application", "call")
    val receiveMember = MemberName("io.ktor.request", "receive")
    val respondMember = MemberName("io.ktor.request", "respond")
    val respondText = MemberName("io.ktor.response", "respondText")
    val noContentMember = MemberName(HttpStatusCode::class.asTypeName(), "NoContent")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val apiName = classDeclaration.findParentDomain().name
    val baseName = apiName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
    val controllerName = apiName.plus("Controller").replaceFirstChar { it.lowercase(Locale.getDefault()) }
    val apiObjectName = apiName.plus("Api")

    fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
      addController(baseName, controllerName)
    }.build())
  }

  private fun TypeSpec.Builder.addController(baseName: BaseName, controllerName: String) =
    addFunction(FunSpec.builder(controllerName).apply {
      receiver(Routing::class)
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/$baseName") {
          addCreateRoute(baseName)
          addControlFlow("%M(%S)", routeMember, "/{id}") {
            addReadRoute()
            addUpdateRoute(baseName)
            addDeleteRoute()
          }
        }
      }
    }.build())

  private fun CodeBlock.Builder.addCreateRoute(baseName: BaseName) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", postMember) {
        addStatement("val request = %M.%M<%T>()", callMember, receiveMember, baseName.toCreateRequestClass())
        addStatement("val result = dao.create(request)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addReadRoute() {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", getMember) {
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("val result = dao.read(id)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addUpdateRoute(baseName: BaseName) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", putMember) {
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("val request = %M.%M<%T>()", callMember, receiveMember, baseName.toUpdateRequestClass())
        addStatement("val result = dao.update(id, request)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addDeleteRoute() {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", deleteMember) {
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("dao.delete(id)")
        addStatement("%M.%M(%T.%M)", callMember, respondMember, HttpStatusCode::class, noContentMember)
      }
    }.build())
  }
}
