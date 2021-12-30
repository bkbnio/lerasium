package io.bkbn.stoik.exposed

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Column(val name: String = "")
