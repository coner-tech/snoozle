package org.coner.snoozle.db

import kotlin.reflect.KClass

class EntitiesManifest(
        op: EntitiesManifest.(Map<KClass<*>, EntityResource<*>>) -> Unit
) {
    private val entities = mutableMapOf<KClass<*>, EntityResource<*>>()

    init {
        this.op(entities)
    }

    fun <E : Entity> entity(op: EntityDefinition<E>.() -> Unit) {
        
    }
}