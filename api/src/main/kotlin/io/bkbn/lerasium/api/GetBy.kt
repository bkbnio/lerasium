package io.bkbn.lerasium.api

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class GetBy(val unique: Boolean = false, vararg val fields: String)
