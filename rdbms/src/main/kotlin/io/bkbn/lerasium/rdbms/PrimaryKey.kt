package io.bkbn.lerasium.rdbms

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PrimaryKey(vararg val fields: String)
