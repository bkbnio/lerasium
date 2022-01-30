package io.bkbn.stoik.core.dao

import io.bkbn.stoik.core.model.Entity
import io.bkbn.stoik.core.model.Request
import io.bkbn.stoik.core.model.Response
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
}
