@file:OptIn(KspExperimental::class)

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
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.api.processor.Members.authenticateMember
import io.bkbn.lerasium.api.processor.Members.callMember
import io.bkbn.lerasium.api.processor.Members.deleteMember
import io.bkbn.lerasium.api.processor.Members.getMember
import io.bkbn.lerasium.api.processor.Members.postMember
import io.bkbn.lerasium.api.processor.Members.putMember
import io.bkbn.lerasium.api.processor.Members.receiveMember
import io.bkbn.lerasium.api.processor.Members.respondMember
import io.bkbn.lerasium.api.processor.Members.routeMember
import io.bkbn.lerasium.api.processor.authSlug
import io.bkbn.lerasium.api.processor.hasQueries
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.model.LoginRequest
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toApiDocumentationClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toDaoClass
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.bkbn.lerasium.utils.StringUtils.capitalized
import io.bkbn.lerasium.utils.StringUtils.decapitalized
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import java.util.Locale
import java.util.UUID

class ControllerVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()
    val apiObjectName = domain.name.plus("Controller")
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
      addController(charter)
      addRootRouteFunction(charter)
      addIdRouteFunction(charter)
      if (charter.hasQueries) addQueryRoutesFunction(charter)
      if (charter.isActor) addAuthRoutesFunction(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addController(charter: LerasiumCharter) {
    val controllerName = charter.domain.name.plus("Handler")
      .replaceFirstChar { it.lowercase(Locale.getDefault()) }
    val baseName = charter.domain.name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
    addFunction(FunSpec.builder(controllerName).apply {
      receiver(Route::class)
      addParameter(ParameterSpec.builder("dao", charter.domain.toDaoClass()).build())
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/$baseName") {
          addStatement("rootRoute(dao)")
          addStatement("idRoute(dao)")
          if (charter.hasQueries) addStatement("queryRoutes(dao)")
          if (charter.isActor) addStatement("authRoutes()")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addRootRouteFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("rootRoute").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addParameter(ParameterSpec.builder("dao", charter.domain.toDaoClass()).build())
      addCodeBlock {
        addStatement("%M()", charter.documentationMemberName("rootDocumentation"))
        addCreateRoute(charter)
        addGetAllRoute()
      }
    }.build())
  }

  private fun TypeSpec.Builder.addIdRouteFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("idRoute").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addParameter(ParameterSpec.builder("dao", charter.domain.toDaoClass()).build())
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/{id}") {
          addStatement("%M()", charter.documentationMemberName("idDocumentation"))
          addReadRoute()
          addUpdateRoute(charter)
          addDeleteRoute()
          addRelationalRoutes(charter)
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addQueryRoutesFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("queryRoutes").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addParameter(ParameterSpec.builder("dao", charter.classDeclaration.findParentDomain().toDaoClass()).build())
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
        addStatement(
          "val request = %M.%M<%T>()",
          callMember,
          receiveMember,
          List::class.asClassName().parameterizedBy(charter.apiCreateRequestClass)
        )
        addStatement("val result = dao.create(request)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addGetAllRoute() {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", getMember) {
        addStatement("val chunk = %M.parameters[%S]?.toInt() ?: 100", callMember, "chunk")
        addStatement("val offset = %M.parameters[%S]?.toInt() ?: 0", callMember, "offset")
        addStatement("val result = dao.getAll(chunk, offset)")
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

  private fun CodeBlock.Builder.addUpdateRoute(charter: LerasiumCharter) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", putMember) {
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("val request = %M.%M<%T>()", callMember, receiveMember, charter.apiUpdateRequestClass)
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
        addStatement("%M.%M(%T.NoContent)", callMember, respondMember, HttpStatusCode::class)
      }
    }.build())
  }

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
            addStatement("val result = dao.getAll${name.capitalized()}(id, chunk, offset)")
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
        addStatement("val result = dao.getBy${name.capitalized()}($name)")
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
        addStatement("val result = dao.getBy${name.capitalized()}($name, chunk, offset)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }
  }

  private fun LerasiumCharter.documentationMemberName(methodName: String) =
    MemberName(domain.toApiDocumentationClass(), methodName)
}
