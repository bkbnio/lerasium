package io.bkbn.lerasium.rdbms

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class OneToMany(val refColumn: String)
