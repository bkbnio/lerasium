package io.bkbn.stoik.playground.domain

import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.kmongo.Document
import io.bkbn.stoik.ktor.Api

@Domain("Profile")
private sealed interface ProfileDomain {
  val mood: String
  val viewCount: Long
  val metadata: ProfileMetadata
}

private interface ProfileMetadata {
  val isPrivate: Boolean
  val otherThing: String
}

@Document
private interface ProfileDocument : ProfileDomain

@Api
private interface ProfileApi : ProfileDomain
