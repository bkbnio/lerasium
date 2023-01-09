package io.bkbn.lerasium.permissions

import io.bkbn.bouncer.core.BouncerPolicy
import kotlin.reflect.KClass

annotation class PolicyProvider(val policy: KClass<BouncerPolicy>)
