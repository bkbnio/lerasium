package io.bkbn.stoik.playground.domain

import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.kmongo.Document
import io.bkbn.stoik.ktor.Api

@Domain("Profile")
sealed interface Profile {
  val mood: String
  val viewCount: Long
}

@Document
interface ProfileDocument : Profile

@Api
interface ProfileApi : Profile