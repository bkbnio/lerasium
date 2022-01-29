package io.bkbn.stoik.core.converter

interface ResponseConverter<T, R> {
  fun T.toResponse(): R
}
