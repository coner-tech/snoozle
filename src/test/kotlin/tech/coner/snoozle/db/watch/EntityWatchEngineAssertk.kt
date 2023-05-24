package tech.coner.snoozle.db.watch

import assertk.Assert
import assertk.assertions.prop
import tech.coner.snoozle.db.Key

fun <K : Key> Assert<Watch<K>>.id() = prop(Watch<K>::id)
fun <K : Key> Assert<Watch<K>>.directoryPattern() = prop(Watch<K>::directoryPattern)
fun <K : Key> Assert<Watch<K>>.filePattern() = prop(Watch<K>::filePattern)