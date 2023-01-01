package io.bkbn.lerasium.core.dao

import io.bkbn.lerasium.core.model.CountResponse
import io.bkbn.lerasium.core.model.Entity
import io.bkbn.lerasium.core.model.IORequest
import io.bkbn.lerasium.core.model.IOResponse
import java.util.UUID

interface Dao<ENT, RESP, CRE, UPT>
  where RESP : IOResponse,
        ENT : Entity<RESP>,
        CRE : IORequest.Create,
        UPT : IORequest.Update {
  fun create(requests: List<CRE>): List<RESP>
  fun read(id: UUID): RESP
  fun update(id: UUID, request: UPT): RESP
  fun delete(id: UUID)
  fun countAll(): CountResponse
  fun getAll(chunk: Int, offset: Int): List<RESP>
}
