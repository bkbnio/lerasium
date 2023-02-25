package io.bkbn.lerasium.core.request

import java.util.UUID

sealed interface RequestContext

data class ActorRequestContext<Actor: Enum<*>>(
  val actor: Actor,
  val actorId: UUID
) : RequestContext

object AnonymousRequestContext : RequestContext
