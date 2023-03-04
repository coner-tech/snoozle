package tech.coner.snoozle.db.watch

import assertk.Assert
import assertk.assertions.prop
import tech.coner.snoozle.db.Key

fun <K : Key> Assert<EntityWatchEngine.Watch<K>>.id() = prop(EntityWatchEngine.Watch<K>::id)
fun <K : Key> Assert<EntityWatchEngine.Watch<K>>.directoryPattern() = prop(EntityWatchEngine.Watch<K>::directoryPattern)
fun <K : Key> Assert<EntityWatchEngine.Watch<K>>.filePattern() = prop(EntityWatchEngine.Watch<K>::filePattern)