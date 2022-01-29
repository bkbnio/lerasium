package io.bkbn.stoik.core.converter

interface ResponseConverter<R> {
  fun toResponse(): R
}
