package io.bkbn.lerasium.persistence

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Index(val unique: Boolean = false)
