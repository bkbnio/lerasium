package io.bkbn.lerasium.playground.domain

import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.domain.LerasiumDomain
import io.bkbn.lerasium.mongo.Document

@Api
@Document
@Domain("Project")
interface Project : LerasiumDomain {
  val name: String
  val description: String
}
