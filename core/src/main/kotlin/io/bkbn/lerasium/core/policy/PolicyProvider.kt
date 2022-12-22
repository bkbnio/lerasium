package io.bkbn.lerasium.core.policy

import io.bkbn.bouncer.core.Policy

fun interface PolicyProvider<E: Any, A: Any, R: Any> {
  fun get(): Policy<E, A, R>
}
