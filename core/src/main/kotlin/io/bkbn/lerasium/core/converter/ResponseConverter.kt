package io.bkbn.lerasium.core.converter

interface ResponseConverter<R> {
  fun toResponse(): R
}
