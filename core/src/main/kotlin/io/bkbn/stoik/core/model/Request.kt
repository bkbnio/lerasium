package io.bkbn.stoik.core.model

import io.bkbn.stoik.core.converter.EntityConverter

sealed interface Request<T, E> : EntityConverter<T, E> {
  interface Create<T : Create<T, E>, E> : Request<T, E>
  interface Update<T : Update<T, E>, E> : Request<T, E>
}
