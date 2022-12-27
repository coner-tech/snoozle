package tech.coner.snoozle.util

import assertk.Assert
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.show
import tech.coner.snoozle.db.Key
import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.entity.EntityEvent
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

fun Assert<Path>.doesNotExist(vararg options: LinkOption) = given { actual ->
    if (!Files.notExists(actual, *options)) {
        expected("${show(actual)} to not exist, but it does exist")
    }
}

fun <K : Key, E : Entity<K>> Assert<EntityEvent<K, E>>.state() = prop(EntityEvent<K, E>::state)
fun <K : Key, E : Entity<K>> Assert<EntityEvent<K, E>>.key() = prop(EntityEvent<K, E>::key)
fun <K : Key, E : Entity<K>> Assert<EntityEvent<K, E>>.entity() = prop(EntityEvent<K, E>::entity)