package io.bkbn.lerasium.persistence

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CompositeIndex(val unique: Boolean = false, vararg val fields: String)
