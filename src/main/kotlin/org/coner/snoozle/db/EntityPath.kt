package org.coner.snoozle.db

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EntityPath(val format: String)