package io.bkbn.stoik.exposed

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class VarChar(val size: Int)