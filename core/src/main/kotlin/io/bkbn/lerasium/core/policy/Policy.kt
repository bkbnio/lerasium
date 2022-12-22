package io.bkbn.lerasium.core.policy

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Policy(val provider: KClass<out PolicyProvider<*, *, *>>)
