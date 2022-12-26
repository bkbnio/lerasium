package io.bkbn.lerasium.api.processor

import com.squareup.kotlinpoet.MemberName

object Members {

  val authenticateMember = MemberName("io.ktor.server.auth", "authenticate")
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
  val hmac256Member = MemberName("com.auth0.jwt.algorithms.Algorithm", "HMAC256")
  val jwtMember = MemberName("io.ktor.server.auth.jwt", "jwt")
  val contentNegotiationMember = MemberName("io.ktor.server.plugins.contentnegotiation", "ContentNegotiation")
  val ktorJsonMember = MemberName("io.ktor.serialization.kotlinx.json", "json")
  val kotlinxJsonMember = MemberName("kotlinx.serialization.json", "Json")
  val authenticationMember = MemberName("io.ktor.server.auth", "authentication")
}
