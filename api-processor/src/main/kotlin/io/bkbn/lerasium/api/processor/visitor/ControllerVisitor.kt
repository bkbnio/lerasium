package io.bkbn.lerasium.api.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.api.processor.Members.authenticateMember
import io.bkbn.lerasium.api.processor.Members.callMember
import io.bkbn.lerasium.api.processor.Members.deleteMember
import io.bkbn.lerasium.api.processor.Members.getMember
import io.bkbn.lerasium.api.processor.Members.postMember
import io.bkbn.lerasium.api.processor.Members.principalMember
import io.bkbn.lerasium.api.processor.Members.putMember
import io.bkbn.lerasium.api.processor.Members.receiveMember
import io.bkbn.lerasium.api.processor.Members.respondMember
import io.bkbn.lerasium.api.processor.Members.routeMember
import io.bkbn.lerasium.api.processor.Members.toRequestContextMember
import io.bkbn.lerasium.api.processor.authSlug
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.model.LoginRequest
import io.bkbn.lerasium.core.request.AnonymousRequestContext
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toApiDocumentationClass
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.StringUtils.capitalized
import io.bkbn.lerasium.utils.StringUtils.decapitalized
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.routing.Route
import java.util.Locale
import java.util.UUID

@OptIn(KspExperimental::class)
class ControllerVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val domain = classDeclaration.getDomain()
    val apiObjectName = domain.name.plus("Controller")
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
      addController(charter)
      addRootRouteFunction(charter)
      addIdRouteFunction(charter)
      // if (charter.hasQueries) addQueryRoutesFunction(charter)
      if (charter.isActor) addAuthRoutesFunction(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addController(charter: LerasiumCharter) {
    val controllerName = charter.domain.name.plus("Handler")
      .replaceFirstChar { it.lowercase(Locale.getDefault()) }
    val baseName = charter.domain.name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
    addFunction(FunSpec.builder(controllerName).apply {
      receiver(Route::class)
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/$baseName") {
          addStatement("rootRoute()")
          addStatement("idRoute()")
          // if (charter.hasQueries) addStatement("queryRoutes()")
          if (charter.isActor) addStatement("authRoutes()")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addRootRouteFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("rootRoute").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addStatement("%M()", charter.documentationMemberName("rootDocumentation"))
        addCreateRoute(charter)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addIdRouteFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("idRoute").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/{id}") {
          addStatement("%M()", charter.documentationMemberName("idDocumentation"))
          addReadRoute(charter)
          addUpdateRoute(charter)
          addDeleteRoute(charter)
          // addRelationalRoutes(charter)
        }
      }
    }.build())
  }

  @Suppress("UnusedPrivateMember")
  private fun TypeSpec.Builder.addQueryRoutesFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("queryRoutes").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addQueries(charter)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addAuthRoutesFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("authRoutes").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/auth") {
          addControlFlow("%M(%S)", routeMember, "/login") {
            addStatement("%M()", charter.documentationMemberName("loginDocumentation"))
            addControlFlow("%M", postMember) {
              addStatement("val request = %M.%M<%T>()", callMember, receiveMember, LoginRequest::class)
              addStatement("val token = %T.authenticate(request)", charter.apiServiceClass)
              addStatement("%M.response.headers.append(%T.Authorization, token)", callMember, HttpHeaders::class)
              addStatement("%M.%M(%T.OK)", callMember, respondMember, HttpStatusCode::class)
            }
          }
          addControlFlow("%M(%S)", authenticateMember, charter.authSlug) {
            addControlFlow("%M(%S)", routeMember, "/validate") {
              addStatement("%M()", charter.documentationMemberName("authValidationDocumentation"))
              addControlFlow("%M", getMember) {
                addStatement("call.respond(HttpStatusCode.NoContent)")
              }
            }
          }
        }
      }
    }.build())
  }

  private fun CodeBlock.Builder.addCreateRoute(charter: LerasiumCharter) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", postMember) {
        addContextCall()
        addStatement(
          "val request = %M.%M<%T>()",
          callMember,
          receiveMember,
          charter.apiCreateRequestClass
        )
        addStatement("val result = %T.create(context, request)", charter.apiServiceClass)
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addReadRoute(charter: LerasiumCharter) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", getMember) {
        addContextCall()
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("val result = %T.read(context, id)", charter.apiServiceClass)
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addUpdateRoute(charter: LerasiumCharter) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", putMember) {
        addContextCall()
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("val request = %M.%M<%T>()", callMember, receiveMember, charter.apiUpdateRequestClass)
        addStatement("val result = %T.update(context, id, request)", charter.apiServiceClass)
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addDeleteRoute(charter: LerasiumCharter) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", deleteMember) {
        addContextCall()
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("%T.delete(context, id)", charter.apiServiceClass)
        addStatement("%M.%M(%T.NoContent)", callMember, respondMember, HttpStatusCode::class)
      }
    }.build())
  }

  @Suppress("UnusedPrivateMember")
  private fun CodeBlock.Builder.addRelationalRoutes(charter: LerasiumCharter) {
    charter.classDeclaration.getAllProperties().filter { it.isAnnotationPresent(Relation::class) }.forEach { property ->
      val name = property.simpleName.getShortName()
      add(CodeBlock.builder().apply {
        addControlFlow("%M(%S)", routeMember, "/${name.decapitalized()}") {
          addStatement("%M()", charter.documentationMemberName("${name}RelationDocumentation"))
          addControlFlow("%M", getMember) {
            addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
            addStatement("val chunk = %M.parameters[%S]?.toInt() ?: 100", callMember, "chunk")
            addStatement("val offset = %M.parameters[%S]?.toInt() ?: 0", callMember, "offset")
            addStatement("val result = %T.get${name.capitalized()}(id, chunk, offset)", charter.apiServiceClass)
            addStatement("%M.%M(result)", callMember, respondMember)
          }
        }
      }.build())
    }
  }

  private fun CodeBlock.Builder.addQueries(charter: LerasiumCharter) {
    charter.classDeclaration.getAllProperties().filter { it.isAnnotationPresent(GetBy::class) }.forEach { prop ->
      val getBy = prop.getAnnotationsByType(GetBy::class).first()
      when (getBy.unique) {
        true -> addUniqueQuery(prop, charter)
        false -> addNonUniqueQuery(prop, charter)
      }
    }
  }

  private fun CodeBlock.Builder.addUniqueQuery(prop: KSPropertyDeclaration, charter: LerasiumCharter) {
    val name = prop.simpleName.getShortName()
    addControlFlow("%M(%S)", routeMember, "/$name/{$name}") {
      addStatement("%M()", charter.documentationMemberName("${name}QueryDocumentation"))
      addControlFlow("%M", getMember) {
        addStatement("val $name = call.parameters[%S]!!", name)
        addStatement("val result = %T.getBy${name.capitalized()}($name)", charter.apiServiceClass)
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }
  }

  private fun CodeBlock.Builder.addNonUniqueQuery(prop: KSPropertyDeclaration, charter: LerasiumCharter) {
    val name = prop.simpleName.getShortName()
    addControlFlow("%M(%S)", routeMember, "/$name/{$name}") {
      addStatement("%M()", charter.documentationMemberName("${name}QueryDocumentation"))
      addControlFlow("%M", getMember) {
        addStatement("val $name = call.parameters[%S]!!", name)
        addStatement("val chunk = %M.parameters[%S]?.toInt() ?: 100", callMember, "chunk")
        addStatement("val offset = %M.parameters[%S]?.toInt() ?: 0", callMember, "offset")
        addStatement("val result = %T.getBy${name.capitalized()}($name, chunk, offset)", charter.apiServiceClass)
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }
  }

  private fun LerasiumCharter.documentationMemberName(methodName: String) =
    MemberName(domain.toApiDocumentationClass(), methodName)

  private fun CodeBlock.Builder.addContextCall() {
    addStatement(
      "val context = %M.%M<%T>()?.%M() ?: %T",
      callMember,
      principalMember,
      JWTPrincipal::class,
      toRequestContextMember,
      AnonymousRequestContext::class
    )
  }
}
