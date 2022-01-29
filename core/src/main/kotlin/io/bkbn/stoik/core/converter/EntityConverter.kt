package io.bkbn.stoik.core.converter

interface EntityConverter<T, E> {
  fun T.toEntity(): E
}
