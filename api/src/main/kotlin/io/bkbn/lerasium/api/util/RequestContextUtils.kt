package io.bkbn.lerasium.api.util

import io.bkbn.lerasium.core.request.ActorRequestContext
import io.bkbn.lerasium.core.request.AnonymousRequestContext
import io.bkbn.lerasium.core.request.RequestContext
import io.ktor.server.auth.jwt.JWTPrincipal
import java.util.UUID

object RequestContextUtils {
  inline fun <reified T: Enum<*>> JWTPrincipal.toContext(): RequestContext = payload.claims["id"]
    ?.asString()
    ?.let { ActorRequestContext<T>(
      actor = TODO(),
      actorId = UUID.fromString(it)
    ) }
    ?: AnonymousRequestContext
}
