package io.bkbn.lerasium.api.processor.visitor

import com.auth0.jwt.JWT
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import io.bkbn.lerasium.api.processor.Members.hmac256Member
import io.bkbn.lerasium.api.processor.Members.jwtMember
import io.bkbn.lerasium.api.processor.Members.respondMember
import io.bkbn.lerasium.api.processor.authSlug
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTPrincipal
import java.util.Locale

class ApiConfigVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val domain = classDeclaration.getDomain()
    val charter = LerasiumCharter(domain, classDeclaration)

    if (charter.isActor) fileBuilder.addAuthenticationConfig(charter)
  }

  private fun FileSpec.Builder.addAuthenticationConfig(charter: LerasiumCharter) {
    val funName = "${charter.domain.name}AuthConfig".replaceFirstChar { it.lowercase(Locale.getDefault()) }
    addFunction(FunSpec.builder(funName).apply {
      addModifiers(KModifier.PRIVATE)
      receiver(AuthenticationConfig::class)
      addCodeBlock {
        addControlFlow("val verifierBuilder = %T.require(%M(%S)).apply", JWT::class, hmac256Member, "secret") {
          addStatement("withAudience(%S)", "http://0.0.0.0:8080/hello")
          addStatement("withIssuer(%S)", "http://0.0.0.0:8080/")
        }
        addStatement("val verifier = verifierBuilder.build()")
        addControlFlow("%M(%S)", jwtMember, charter.authSlug) {
          addStatement("realm = %S", "application")
          addStatement("verifier(verifier)")
          addControlFlow("validate { credential ->") {
            addControlFlow("if (credential.payload.getClaim(%S).asString() != %S)", "id", "") {
              addStatement("%T(credential.payload)", JWTPrincipal::class)
            }
            addControlFlow("else") {
              addStatement("null")
            }
          }
          addControlFlow("challenge { _, _ ->") {
            addStatement(
              "call.%M(%T.Unauthorized, %S)",
              respondMember,
              HttpStatusCode::class,
              "Token is not valid or has expired"
            )
          }
        }
      }
    }.build())
  }
}
