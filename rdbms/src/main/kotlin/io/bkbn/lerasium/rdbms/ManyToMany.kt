package io.bkbn.lerasium.rdbms

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class ManyToMany(val clazz: KClass<*>)
