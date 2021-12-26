package io.bkbn.stoik.ktor.core

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Api(val name: String)
