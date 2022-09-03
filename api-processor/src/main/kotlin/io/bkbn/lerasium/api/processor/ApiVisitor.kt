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
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toCreateRequestClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toDaoClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toUpdateRequestClass
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.bkbn.lerasium.utils.StringUtils.capitalized
import io.bkbn.lerasium.utils.StringUtils.decapitalized
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import java.util.Locale
import java.util.UUID

class ApiVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
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

    fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
      addController(classDeclaration, domain)
      addDocumentation(classDeclaration, domain)
    }.build())
  }

  private fun TypeSpec.Builder.addController(cd: KSClassDeclaration, domain: Domain) {
    val controllerName = domain.name.plus("Controller").replaceFirstChar { it.lowercase(Locale.getDefault()) }
    val baseName = domain.name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
    addFunction(FunSpec.builder(controllerName).apply {
      receiver(Route::class)
      addParameter(ParameterSpec.builder("dao", domain.toDaoClass()).build())
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/$baseName") {
          addStatement("rootDocumentation()")
          addCreateRoute(domain)
          addGetAllRoute()
          addControlFlow("%M(%S)", routeMember, "/{id}") {
            addStatement("idDocumentation()")
            addReadRoute()
            addUpdateRoute(domain)
            addDeleteRoute()
            addRelationalRoutes(cd)
          }
          addQueries(cd)
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addDocumentation(classDeclaration: KSClassDeclaration, domain: Domain) {
    addRootDocumentation(domain)
    addIdDocumentation(domain)
    addRelationalDocumentation(classDeclaration, domain)
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

  private fun CodeBlock.Builder.addCreateRoute(domain: Domain) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", postMember) {
        addStatement(
          "val request = %M.%M<%T>()",
          callMember,
          receiveMember,
          List::class.asClassName().parameterizedBy(domain.toCreateRequestClass())
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

  private fun CodeBlock.Builder.addUpdateRoute(domain: Domain) {
    add(CodeBlock.builder().apply {
      addControlFlow("%M", putMember) {
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("val request = %M.%M<%T>()", callMember, receiveMember, domain.toUpdateRequestClass())
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

  private fun CodeBlock.Builder.addRelationalRoutes(cd: KSClassDeclaration) {
    cd.getAllProperties().filter { it.isAnnotationPresent(Relation::class) }.forEach { property ->
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

  private fun TypeSpec.Builder.addRelationalDocumentation(cd: KSClassDeclaration, domain: Domain) {
    cd.getAllProperties().filter { it.isAnnotationPresent(Relation::class) }.forEach { property ->
      val name = property.simpleName.getShortName()
      addFunction(FunSpec.builder("install${name.capitalized()}Documentation").apply {
        receiver(Route::class)
        addModifiers(KModifier.PRIVATE)
        addCodeBlock {
          addControlFlow("%M(%T())", installMember, NotarizedRoute::class) {
            addStatement("tags = setOf(%S)", domain.name)
            addControlFlow("get = %T.builder", GetInfo::class) {
              addStatement("summary(%S)", "Get All ${domain.name} ${name.capitalized()}")
              addStatement(
                "description(%S)",
                """
                  Retrieves a paginated list of ${name.capitalized()} entities associated
                  with the provided ${domain.name}
                """.trimIndent()
              )
              addStatement("parameters(*%M().toTypedArray().plus(%M()))", getAllParametersMember, idParameterMember)
              addControlFlow("response") {
                addStatement("responseType<%T>()", List::class.asTypeName().parameterizedBy(domain.toResponseClass()))
                addStatement("responseCode(%T.OK)", HttpStatusCode::class)
                addStatement("description(%S)", "Paginated list of ${domain.name} entities")
              }
            }
          }
        }
      }.build())
    }
  }

  private fun CodeBlock.Builder.addQueries(cd: KSClassDeclaration) {
    cd.getAllProperties().filter { it.isAnnotationPresent(GetBy::class) }.forEach { prop ->
      val getBy = prop.getAnnotationsByType(GetBy::class).first()
      when (getBy.unique) {
        true -> addUniqueQuery(prop)
        false -> addNonUniqueQuery(prop)
      }
    }
  }

  private fun CodeBlock.Builder.addUniqueQuery(prop: KSPropertyDeclaration) {
    val name = prop.simpleName.getShortName()
    addControlFlow("%M(%S)", routeMember, "/$name/{$name}") {
      addControlFlow("%M", getMember) {
        addStatement("val $name = call.parameters[%S]!!", name)
        addStatement("val result = dao.getBy${name.capitalized()}($name)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }
  }

  private fun CodeBlock.Builder.addNonUniqueQuery(prop: KSPropertyDeclaration) {
    val name = prop.simpleName.getShortName()
    addControlFlow("%M(%S)", routeMember, "/$name/{$name}") {
      addControlFlow("%M", getMember) {
        addStatement("val $name = call.parameters[%S]!!", name)
        addStatement("val chunk = %M.parameters[%S]?.toInt() ?: 100", callMember, "chunk")
        addStatement("val offset = %M.parameters[%S]?.toInt() ?: 0", callMember, "offset")
        addStatement("val result = dao.getBy${name.capitalized()}($name, chunk, offset)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }
  }
}
