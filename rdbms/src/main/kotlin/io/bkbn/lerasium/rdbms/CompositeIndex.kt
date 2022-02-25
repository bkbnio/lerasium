package io.bkbn.lerasium.rdbms

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CompositeIndex(val unique: Boolean = false, vararg val fields: String)
