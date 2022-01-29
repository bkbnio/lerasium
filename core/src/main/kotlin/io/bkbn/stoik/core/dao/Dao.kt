package io.bkbn.stoik.core.dao

import io.bkbn.stoik.core.model.Entity
import io.bkbn.stoik.core.model.Request
import io.bkbn.stoik.core.model.Response
import java.util.UUID

interface Dao<RESP, ENT, CRE, UPT>
  where RESP : Response,
        ENT : Entity<ENT, RESP>,
        CRE : Request.Create<CRE, ENT>,
        UPT : Request.Update<UPT, ENT> {
  fun create(request: Request.Create<CRE, ENT>): RESP
  fun read(id: UUID): RESP
  fun update(id: UUID, request: Request.Update<UPT, ENT>): RESP
  fun delete(id: UUID)
}
