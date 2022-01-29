package io.bkbn.stoik.core.converter

interface EntityConverter<E> {
  fun toEntity(): E
}
