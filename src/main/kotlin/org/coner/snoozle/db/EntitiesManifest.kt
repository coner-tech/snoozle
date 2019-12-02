package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Path
import kotlin.reflect.KClass

class EntitiesManifest(
        val root: Path,
        op: EntitiesManifest.(Map<KClass<*>, EntityResource<*>>) -> Unit,
        val objectMapper: ObjectMapper
) {
    val entityResources = mutableMapOf<KClass<*>, EntityResource<*>>()

    init {
        this.op(entityResources)
    }

    inline fun <reified E : Entity> entity(op: EntityDefinition<E>.() -> Unit) {
        entityResources[E::class] = EntityResource(
                root = root,
                entityDefinition = EntityDefinition<E>().apply(op),
                objectMapper = objectMapper
        )
    }
}