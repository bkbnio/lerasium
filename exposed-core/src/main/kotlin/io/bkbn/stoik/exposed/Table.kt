package io.bkbn.stoik.exposed

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Table(val name: String)
