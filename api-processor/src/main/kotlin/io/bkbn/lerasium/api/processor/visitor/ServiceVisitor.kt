package io.bkbn.lerasium.api.processor.visitor

import com.auth0.jwt.JWT
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.api.processor.Members.hmac256Member
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.model.LoginRequest
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.LerasiumUtils.isDomain
import java.util.Date
import java.util.UUID

@OptIn(KspExperimental::class)
class ServiceVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val domain = classDeclaration.getDomain()
    val apiObjectName = domain.name.plus("Service")
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
      addService(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addService(charter: LerasiumCharter) {
    addCreateFunction(charter)
    addReadFunction(charter)
    addUpdateFunction(charter)
    addDeleteFunction(charter)
    if (charter.isActor) addAuthenticationFunction(charter)
  }

  private fun TypeSpec.Builder.addCreateFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("create").apply {
      addModifiers(KModifier.SUSPEND)
      addParameter("request", charter.apiCreateRequestClass)
      returns(charter.apiResponseClass)
      addCodeBlock {
        addStatement("val result = %T.create(request)", charter.repositoryClass)
        addStatement("return %T.from(result)", charter.apiResponseClass)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addReadFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("read").apply {
      addModifiers(KModifier.SUSPEND)
      addParameter("id", UUID::class)
      returns(charter.apiResponseClass)
      addCodeBlock {
        addStatement("val result = %T.read(id)", charter.repositoryClass)
        addStatement("return %T.from(result)", charter.apiResponseClass)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("update").apply {
      addModifiers(KModifier.SUSPEND)
      addParameter("id", UUID::class)
      addParameter("request", charter.apiUpdateRequestClass)
      returns(charter.apiResponseClass)
      addCodeBlock {
        addStatement("val result = %T.update(id, request)", charter.repositoryClass)
        addStatement("return %T.from(result)", charter.apiResponseClass)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addDeleteFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("delete").apply {
      addModifiers(KModifier.SUSPEND)
      addParameter("id", UUID::class)
      addCodeBlock {
        addStatement("%T.delete(id)", charter.repositoryClass)
      }
    }.build())
  }

  private fun TypeSpec.Builder.addAuthenticationFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("authenticate").apply {
      addModifiers(KModifier.SUSPEND)
      addParameter("request", LoginRequest::class)
      returns(String::class)
      addCodeBlock {
        addStatement("val actor = %T.authenticate(request.username, request.password)", charter.repositoryClass)
        addControlFlow("val unsignedToken = %T.create().apply", JWT::class) {
          addStatement("withAudience(%S)", "http://0.0.0.0:8080/hello")
          addStatement("withIssuer(%S)", "http://0.0.0.0:8080/")
          addStatement("withClaim(%S, actor.id.toString())", "id")
          addStatement("withExpiresAt(%T(%T.currentTimeMillis() + 60000))", Date::class, System::class)
        }
        addStatement("return unsignedToken.sign(%M(%S))", hmac256Member, "secret")
      }
    }.build())
  }
}
