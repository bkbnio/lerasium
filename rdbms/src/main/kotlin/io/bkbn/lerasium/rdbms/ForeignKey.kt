package io.bkbn.lerasium.rdbms

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class ForeignKey(val domain: String, val field: String = "id")
