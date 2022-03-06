package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.mongo.Document
import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.persistence.CompositeIndex

@Domain("Profile")
private sealed interface Profile {
  val mood: String?
  val viewCount: Long
  val metadata: ProfileMetadata
}

private interface ProfileMetadata {
  val isPrivate: Boolean
  val otherThing: String
}

@Document
@CompositeIndex(fields = ["mood", "viewCount"])
private interface ProfileDocument : Profile

@Api
private interface ProfileApi : Profile
