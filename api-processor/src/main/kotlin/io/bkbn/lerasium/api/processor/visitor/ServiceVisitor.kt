package io.bkbn.lerasium.api.processor.visitor

import com.auth0.jwt.JWT
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.api.processor.Members.hmac256Member
import io.bkbn.lerasium.core.model.LoginRequest
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import java.util.Date

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
    if (charter.isActor) addAuthenticationFunction(charter)
  }

  private fun TypeSpec.Builder.addAuthenticationFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("authenticate").apply {
      addParameter("request", LoginRequest::class)
      returns(String::class)
      addCodeBlock {
        addStatement("val actor = %T.authenticate(request.username, request.password)", charter.daoClass)
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
