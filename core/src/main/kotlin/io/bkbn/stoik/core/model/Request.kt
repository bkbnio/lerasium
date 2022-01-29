package io.bkbn.stoik.core.model

import io.bkbn.stoik.core.converter.EntityConverter

sealed interface Request<T, E> : EntityConverter<T, E> {
  interface Create<T, E> : Request<T, E> where T : Create<T, E>
  interface Update<T, E> : Request<T, E> where T : Update<T, E>
}
