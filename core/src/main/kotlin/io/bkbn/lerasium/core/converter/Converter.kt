package io.bkbn.lerasium.core.converter

interface Converter<A, B> {

  fun from(input: A): B

}
