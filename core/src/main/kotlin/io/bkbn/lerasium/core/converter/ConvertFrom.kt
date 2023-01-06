package io.bkbn.lerasium.core.converter

interface ConvertFrom<A, B> {

  fun from(input: A): B

}
