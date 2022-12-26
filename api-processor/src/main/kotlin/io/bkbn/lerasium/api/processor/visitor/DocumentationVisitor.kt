package io.bkbn.lerasium.api.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.kompendium.core.metadata.DeleteInfo
import io.bkbn.kompendium.core.metadata.GetInfo
import io.bkbn.kompendium.core.metadata.PostInfo
import io.bkbn.kompendium.core.metadata.PutInfo
import io.bkbn.kompendium.core.plugin.NotarizedRoute
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.api.processor.Members.getAllParametersMember
import io.bkbn.lerasium.api.processor.Members.idParameterMember
import io.bkbn.lerasium.api.processor.Members.installMember
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.model.LoginRequest
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toCreateRequestClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toUpdateRequestClass
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.bkbn.lerasium.utils.StringUtils.capitalized
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route

class DocumentationVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()
    val apiObjectName = domain.name.plus("Documentation")
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
      addRootDocumentation(charter.domain)
      addIdDocumentation(charter.domain)
      addRelationalDocumentation(charter)
      addQueryDocumentation(charter)
      if (charter.isActor) addAuthDocumentation(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addRootDocumentation(domain: Domain) {
    addFunction(FunSpec.builder("rootDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.INTERNAL)
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
      addModifiers(KModifier.INTERNAL)
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

  @OptIn(KspExperimental::class)
  private fun TypeSpec.Builder.addRelationalDocumentation(charter: LerasiumCharter) {
    charter.cd.getAllProperties().filter { it.isAnnotationPresent(Relation::class) }.forEach { property ->
      val name = property.simpleName.getShortName()
      addFunction(FunSpec.builder("${name}RelationDocumentation").apply {
        receiver(Route::class)
        addModifiers(KModifier.INTERNAL)
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

  @OptIn(KspExperimental::class)
  private fun TypeSpec.Builder.addQueryDocumentation(charter: LerasiumCharter) {
    charter.cd.getAllProperties().filter { it.isAnnotationPresent(GetBy::class) }.forEach { prop ->
      val getBy = prop.getAnnotationsByType(GetBy::class).first()
      when (getBy.unique) {
        true -> addUniqueQueryDocumentation(prop, charter.domain)
        false -> addNonUniqueQueryDocumentation(prop, charter.domain)
      }
    }
  }

  private fun TypeSpec.Builder.addUniqueQueryDocumentation(prop: KSPropertyDeclaration, domain: Domain) {
    val name = prop.simpleName.getShortName()
    addFunction(FunSpec.builder("${name}QueryDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.INTERNAL)
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

  private fun TypeSpec.Builder.addNonUniqueQueryDocumentation(prop: KSPropertyDeclaration, domain: Domain) {
    val name = prop.simpleName.getShortName()
    addFunction(FunSpec.builder("${name}QueryDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.INTERNAL)
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

  private fun TypeSpec.Builder.addAuthDocumentation(charter: LerasiumCharter) {
    addLoginDocumentation(charter)
    addAuthValidationDocumentation(charter)
  }

  private fun TypeSpec.Builder.addLoginDocumentation(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("loginDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.INTERNAL)
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

  private fun TypeSpec.Builder.addAuthValidationDocumentation(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("authValidationDocumentation").apply {
      receiver(Route::class)
      addModifiers(KModifier.INTERNAL)
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
}
