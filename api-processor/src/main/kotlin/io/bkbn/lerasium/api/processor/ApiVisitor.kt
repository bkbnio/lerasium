@file:OptIn(KspExperimental::class)

package io.bkbn.lerasium.api.processor

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
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.kompendium.core.metadata.DeleteInfo
import io.bkbn.kompendium.core.metadata.GetInfo
import io.bkbn.kompendium.core.metadata.PostInfo
import io.bkbn.kompendium.core.metadata.PutInfo
import io.bkbn.kompendium.core.plugin.NotarizedRoute
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.core.Actor
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.model.LoginRequest
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toAuthTag
import io.bkbn.lerasium.utils.KotlinPoetUtils.toCreateRequestClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toDaoClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toUpdateRequestClass
import io.bkbn.lerasium.utils.LerasiumUtils.findParent
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.bkbn.lerasium.utils.StringUtils.capitalized
import io.bkbn.lerasium.utils.StringUtils.decapitalized
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import java.util.Locale
import java.util.UUID

class ApiVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    val authenticationMember = MemberName("io.ktor.server.auth", "authenticate")
    val routeMember = MemberName("io.ktor.server.routing", "route")
    val getMember = MemberName("io.ktor.server.routing", "get")
    val postMember = MemberName("io.ktor.server.routing", "post")
    val putMember = MemberName("io.ktor.server.routing", "put")
    val deleteMember = MemberName("io.ktor.server.routing", "delete")
    val callMember = MemberName("io.ktor.server.application", "call")
    val receiveMember = MemberName("io.ktor.server.request", "receive")
    val respondMember = MemberName("io.ktor.server.response", "respond")
    val installMember = MemberName("io.ktor.server.application", "install")
    val getAllParametersMember = MemberName("io.bkbn.lerasium.api.util.ApiDocumentationUtils", "getAllParameters")
    val idParameterMember = MemberName("io.bkbn.lerasium.api.util.ApiDocumentationUtils", "idParameter")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()
    val apiObjectName = domain.name.plus("Api")
    val charter = ApiCharter(domain, classDeclaration)

    fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
      addController(charter)
      addRootRouteFunction(charter)
      addIdRouteFunction(charter)
      if (charter.hasQueries) addQueryRoutesFunction(charter)
      if (charter.isActor) addAuthRoutesFunction(charter)
      addDocumentation(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addController(charter: ApiCharter) {
    val controllerName = charter.domain.name.plus("Controller")
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
          if (charter.isActor) addStatement("authRoutes(dao)")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addRootRouteFunction(charter: ApiCharter) {
    addFunction(FunSpec.builder("rootRoute").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addParameter(ParameterSpec.builder("dao", charter.domain.toDaoClass()).build())
      addCodeBlock {
        addStatement("rootDocumentation()")
        addCreateRoute(charter)
        addGetAllRoute()
      }
    }.build())
  }

  private fun TypeSpec.Builder.addIdRouteFunction(charter: ApiCharter) {
    addFunction(FunSpec.builder("idRoute").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addParameter(ParameterSpec.builder("dao", charter.domain.toDaoClass()).build())
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/{id}") {
          addStatement("idDocumentation()")
          addReadRoute()
          addUpdateRoute(charter)
          addDeleteRoute()
          addRelationalRoutes(charter)
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addQueryRoutesFunction(charter: ApiCharter) {
    addFunction(FunSpec.builder("queryRoutes").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addParameter(ParameterSpec.builder("dao", charter.cd.findParentDomain().toDaoClass()).build())
      addCodeBlock {
        addQueries(charter)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addAuthRoutesFunction(charter: ApiCharter) {
    addFunction(FunSpec.builder("authRoutes").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addParameter(ParameterSpec.builder("dao", charter.cd.findParentDomain().toDaoClass()).build())
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/auth") {
          addControlFlow("%M(%S)", routeMember, "/login") {
            addStatement("loginDocumentation()")
            addControlFlow("%M", postMember) {
              addStatement("call.respond(HttpStatusCode.NotImplemented)")
            }
          }
          addControlFlow("%M(%S)", authenticationMember, charter.domain.toAuthTag()) {
            addControlFlow("%M(%S)", routeMember, "/validate") {
              addStatement("authValidationDocumentation()")
              addControlFlow("%M", getMember) {
                addStatement("call.respond(HttpStatusCode.NoContent)")
              }
            }
          }
          // todo validate
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addDocumentation(charter: ApiCharter) {
    addRootDocumentation(charter.domain)
    addIdDocumentation(charter.domain)
    addRelationalDocumentation(charter)
    addQueryDocumentation(charter)
    if (charter.isActor) addAuthDocumentation(charter)
  }

  private fun TypeSpec.Builder.addRootDocumentation(domain: Domain) {
    addFunction(FunSpec.builder("rootDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addControlFlow("%M(%T())", installMember, NotarizedRoute::class) {
          addStatement("tags = setOf(%S)", domain.name)
          addControlFlow("get = %T.builder", GetInfo::class) {
            addStatement("summary(%S)", "Get All ${domain.name} Entities")
            addStatement("description(%S)", "Retrieves a paginated list of ${domain.name} Entities")
            addStatement("parameters(*%M().toTypedArray())", getAllParametersMember)
            addControlFlow("response") {
              addStatement("responseType<%T>()", List::class.asTypeName().parameterizedBy(domain.toResponseClass()))
              addStatement("responseCode(%T.OK)", HttpStatusCode::class)
              addStatement("description(%S)", "Paginated list of ${domain.name} entities")
            }
          }
          addControlFlow("post = %T.builder", PostInfo::class) {
            addStatement("summary(%S)", "Create New ${domain.name} Entity")
            addStatement("description(%S)", "Persists a new ${domain.name} entity in the database")
            addControlFlow("response") {
              addStatement("responseType<%T>()", List::class.asTypeName().parameterizedBy(domain.toResponseClass()))
              addStatement("responseCode(%T.Created)", HttpStatusCode::class)
              addStatement("description(%S)", "${domain.name} entities saved successfully")
            }
            addControlFlow("request") {
              addStatement("requestType<%T>()", List::class.asTypeName().parameterizedBy(domain.toCreateRequestClass()))
              addStatement("description(%S)", "Collection of ${domain.name} entities the user wishes to persist")
            }
          }
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addIdDocumentation(domain: Domain) {
    addFunction(FunSpec.builder("idDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addControlFlow("%M(%T())", installMember, NotarizedRoute::class) {
          addStatement("tags = setOf(%S)", domain.name)
          addStatement("parameters = %M()", idParameterMember)
          addControlFlow("get = %T.builder", GetInfo::class) {
            addStatement("summary(%S)", "Get ${domain.name} by ID")
            addStatement("description(%S)", "Retrieves the specified ${domain.name} entity by its ID")
            addControlFlow("response") {
              addStatement("responseType<%T>()", domain.toResponseClass())
              addStatement("responseCode(%T.OK)", HttpStatusCode::class)
              addStatement("description(%S)", "The ${domain.name} entity with the specified ID")
            }
          }
          addControlFlow("put = %T.builder", PutInfo::class) {
            addStatement("summary(%S)", "Update ${domain.name} by ID")
            addStatement("description(%S)", "Updates the specified ${domain.name} entity by its ID")
            addControlFlow("request") {
              addStatement("requestType<%T>()", domain.toUpdateRequestClass())
              addStatement("description(%S)", "Fields that can be updated on the ${domain.name} entity")
            }
            addControlFlow("response") {
              addStatement("responseType<%T>()", domain.toResponseClass())
              addStatement("responseCode(%T.Created)", HttpStatusCode::class)
              addStatement("description(%S)", "Indicates that the ${domain.name} entity was updated successfully")
            }
          }
          addControlFlow("delete = %T.builder", DeleteInfo::class) {
            addStatement("summary(%S)", "Delete ${domain.name} by ID")
            addStatement("description(%S)", "Deletes the specified ${domain.name} entity by its ID")
            addControlFlow("response") {
              addStatement("responseType<%T>()", Unit::class)
              addStatement("responseCode(%T.NoContent)", HttpStatusCode::class)
              addStatement("description(%S)", "Indicates that the ${domain.name} entity was deleted successfully")
            }
          }
        }
      }
    }.build())
  }

  private fun CodeBlock.Builder.addCreateRoute(charter: ApiCharter) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", postMember) {
        addStatement(
          "val request = %M.%M<%T>()",
          callMember,
          receiveMember,
          List::class.asClassName().parameterizedBy(charter.domain.toCreateRequestClass())
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

  private fun CodeBlock.Builder.addUpdateRoute(charter: ApiCharter) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", putMember) {
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("val request = %M.%M<%T>()", callMember, receiveMember, charter.domain.toUpdateRequestClass())
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

  private fun CodeBlock.Builder.addRelationalRoutes(charter: ApiCharter) {
    charter.cd.getAllProperties().filter { it.isAnnotationPresent(Relation::class) }.forEach { property ->
      val name = property.simpleName.getShortName()
      add(CodeBlock.builder().apply {
        addControlFlow("%M(%S)", routeMember, "/${name.decapitalized()}") {
          addStatement("install${name.capitalized()}Documentation()")
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

  private fun TypeSpec.Builder.addRelationalDocumentation(charter: ApiCharter) {
    charter.cd.getAllProperties().filter { it.isAnnotationPresent(Relation::class) }.forEach { property ->
      val name = property.simpleName.getShortName()
      addFunction(FunSpec.builder("install${name.capitalized()}Documentation").apply {
        receiver(Route::class)
        addModifiers(KModifier.PRIVATE)
        addCodeBlock {
          addControlFlow("%M(%T())", installMember, NotarizedRoute::class) {
            addStatement("tags = setOf(%S)", charter.domain.name)
            addControlFlow("get = %T.builder", GetInfo::class) {
              addStatement("summary(%S)", "Get All ${charter.domain.name} ${name.capitalized()}")
              addStatement(
                "description(%S)",
                """
                  Retrieves a paginated list of ${name.capitalized()} entities associated
                  with the provided ${charter.domain.name}
                """.trimIndent()
              )
              addStatement("parameters(*%M().toTypedArray().plus(%M()))", getAllParametersMember, idParameterMember)
              addControlFlow("response") {
                addStatement(
                  "responseType<%T>()",
                  List::class.asTypeName().parameterizedBy(charter.domain.toResponseClass())
                )
                addStatement("responseCode(%T.OK)", HttpStatusCode::class)
                addStatement("description(%S)", "Paginated list of ${charter.domain.name} entities")
              }
            }
          }
        }
      }.build())
    }
  }

  private fun CodeBlock.Builder.addQueries(charter: ApiCharter) {
    charter.cd.getAllProperties().filter { it.isAnnotationPresent(GetBy::class) }.forEach { prop ->
      val getBy = prop.getAnnotationsByType(GetBy::class).first()
      when (getBy.unique) {
        true -> addUniqueQuery(prop)
        false -> addNonUniqueQuery(prop)
      }
    }
  }

  private fun TypeSpec.Builder.addQueryDocumentation(charter: ApiCharter) {
    charter.cd.getAllProperties().filter { it.isAnnotationPresent(GetBy::class) }.forEach { prop ->
      val getBy = prop.getAnnotationsByType(GetBy::class).first()
      when (getBy.unique) {
        true -> addUniqueQueryDocumentation(prop, charter.domain)
        false -> addNonUniqueQueryDocumentation(prop, charter.domain)
      }
    }
  }

  private fun TypeSpec.Builder.addAuthDocumentation(charter: ApiCharter) {
    addLoginDocumentation(charter)
    addAuthValidationDocumentation(charter)
  }

  private fun TypeSpec.Builder.addLoginDocumentation(charter: ApiCharter) {
    addFunction(FunSpec.builder("loginDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addControlFlow("%M(%T())", installMember, NotarizedRoute::class) {
          addStatement("tags = setOf(%S)", charter.domain.name)
          addControlFlow("post = %T.builder", PostInfo::class) {
            addStatement("summary(%S)", "Login")
            addStatement("description(%S)", "Authenticates the ${charter.domain.name}")
            addControlFlow("request") {
              addStatement("requestType<%T>()", LoginRequest::class)
              addStatement("description(%S)", "The username and password of the ${charter.domain.name}")
            }
            addControlFlow("response") {
              addStatement("responseType<%T>()", Unit::class)
              addStatement("responseCode(%T.NoContent)", HttpStatusCode::class)
              addStatement("description(%S)", "Indicates successful authentication, token is returned in header")
            }
          }
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addAuthValidationDocumentation(charter: ApiCharter) {
    addFunction(FunSpec.builder("authValidationDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addControlFlow("%M(%T())", installMember, NotarizedRoute::class) {
          addStatement("tags = setOf(%S)", charter.domain.name)
          addControlFlow("get = %T.builder", GetInfo::class) {
            addStatement("summary(%S)", "Auth Validation")
            addStatement("description(%S)", "Validate the current auth token")
            addControlFlow("response") {
              addStatement("responseType<%T>()", Unit::class)
              addStatement("responseCode(%T.NoContent)", HttpStatusCode::class)
              addStatement("description(%S)", "Auth validation response")
            }
            addControlFlow("canRespond") {
              addStatement("responseType<%T>()", Unit::class)
              addStatement("responseCode(%T.Unauthorized)", HttpStatusCode::class)
              addStatement("description(%S)", "Token is invalid")
            }
          }
        }
      }
    }.build())
  }

  private fun CodeBlock.Builder.addUniqueQuery(prop: KSPropertyDeclaration) {
    val name = prop.simpleName.getShortName()
    addControlFlow("%M(%S)", routeMember, "/$name/{$name}") {
      addStatement("install${name.capitalized()}QueryDocumentation()")
      addControlFlow("%M", getMember) {
        addStatement("val $name = call.parameters[%S]!!", name)
        addStatement("val result = dao.getBy${name.capitalized()}($name)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }
  }

  private fun TypeSpec.Builder.addUniqueQueryDocumentation(prop: KSPropertyDeclaration, domain: Domain) {
    val name = prop.simpleName.getShortName()
    addFunction(FunSpec.builder("install${name.capitalized()}QueryDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addControlFlow("%M(%T())", installMember, NotarizedRoute::class) {
          addStatement("tags = setOf(%S)", domain.name)
          addControlFlow("get = %T.builder", GetInfo::class) {
            addStatement("summary(%S)", "Get ${domain.name} by ${name.capitalized()}")
            addStatement(
              "description(%S)",
              """
              Attempts to find a ${domain.name} entity associated
              with the provided ${name.capitalized()} id
              """.trimIndent()
            )
            addStatement("parameters(*%M().toTypedArray())", idParameterMember)
            addControlFlow("response") {
              addStatement("responseType<%T>()", domain.toResponseClass())
              addStatement("responseCode(%T.OK)", HttpStatusCode::class)
              addStatement("description(%S)", "${domain.name} entity associated with the specified $name")
            }
          }
        }
      }
    }.build())
  }

  private fun CodeBlock.Builder.addNonUniqueQuery(prop: KSPropertyDeclaration) {
    val name = prop.simpleName.getShortName()
    addControlFlow("%M(%S)", routeMember, "/$name/{$name}") {
      addStatement("install${name.capitalized()}QueryDocumentation()")
      addControlFlow("%M", getMember) {
        addStatement("val $name = call.parameters[%S]!!", name)
        addStatement("val chunk = %M.parameters[%S]?.toInt() ?: 100", callMember, "chunk")
        addStatement("val offset = %M.parameters[%S]?.toInt() ?: 0", callMember, "offset")
        addStatement("val result = dao.getBy${name.capitalized()}($name, chunk, offset)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }
  }

  private fun TypeSpec.Builder.addNonUniqueQueryDocumentation(prop: KSPropertyDeclaration, domain: Domain) {
    val name = prop.simpleName.getShortName()
    addFunction(FunSpec.builder("install${name.capitalized()}QueryDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.PRIVATE)
      addCodeBlock {
        addControlFlow("%M(%T())", installMember, NotarizedRoute::class) {
          addStatement("tags = setOf(%S)", domain.name)
          addControlFlow("get = %T.builder", GetInfo::class) {
            addStatement("summary(%S)", "Get All ${domain.name} by ${name.capitalized()}")
            addStatement(
              "description(%S)",
              """
              Attempts to find all ${domain.name} entities associated
              with the provided ${name.capitalized()} id
              """.trimIndent()
            )
            addStatement("parameters(*%M().toTypedArray().plus(%M()))", getAllParametersMember, idParameterMember)
            addControlFlow("response") {
              addStatement("responseType<%T>()", List::class.asTypeName().parameterizedBy(domain.toResponseClass()))
              addStatement("responseCode(%T.OK)", HttpStatusCode::class)
              addStatement("description(%S)", "${domain.name} entities associated with the specified $name")
            }
          }
        }
      }
    }.build())
  }

  private class ApiCharter(val domain: Domain, val cd: KSClassDeclaration) {
    val isActor: Boolean = cd.findParent().isAnnotationPresent(Actor::class)
    val hasQueries: Boolean = cd.getAllProperties().any { it.isAnnotationPresent(GetBy::class) }
  }
}
