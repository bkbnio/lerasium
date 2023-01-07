package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.domain.LerasiumDomain
import io.bkbn.lerasium.mongo.Document
import io.bkbn.lerasium.persistence.CompositeIndex
import io.bkbn.lerasium.persistence.Index

@Api
@Document
@CompositeIndex(fields = ["mood", "viewCount"])
@Domain("Profile")
interface Profile : LerasiumDomain {
  val mood: String?
  val viewCount: Long
  val metadata: Metadata
  val miscA: Misc
  // val miscB: Misc // TODO Handle this

  interface Metadata {
    val isPrivate: Boolean
    val otherThing: String
    @Index(unique = true)
    val handle: String
  }

  interface Misc {
    val infoA: String
    val infoB: Int
    val more: MoreMisc

    interface MoreMisc {
      val infoC: String
      val infoD: Int
    }
  }
}
