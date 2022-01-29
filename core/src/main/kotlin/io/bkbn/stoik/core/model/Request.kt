package io.bkbn.stoik.core.model

import io.bkbn.stoik.core.converter.EntityConverter

sealed interface Request<E> : EntityConverter<E> {
  interface Create<E> : Request<E>
  interface Update<E> : Request<E>
}
