package io.bkbn.lerasium.core.dao

import io.bkbn.lerasium.core.model.CountResponse
import io.bkbn.lerasium.core.model.Entity
import io.bkbn.lerasium.core.model.Request
import io.bkbn.lerasium.core.model.Response
import java.util.UUID

interface Dao<ENT, RESP, CRE, UPT>
  where RESP : Response,
        ENT : Entity<RESP>,
        CRE : Request.Create,
        UPT : Request.Update {
  fun create(request: CRE): RESP
  fun read(id: UUID): RESP
  fun update(id: UUID, request: UPT): RESP
  fun delete(id: UUID)
  fun countAll(): CountResponse
}
