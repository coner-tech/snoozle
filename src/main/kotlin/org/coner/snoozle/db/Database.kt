package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class Database(
        root: File,
        vararg entities: KClass<out Entity>,
        objectMapper: ObjectMapper = jacksonObjectMapper()
) {

    val resources: Map<KClass<Entity>, Resource<Entity>>

    init {
        val resourcesBuilder = mutableMapOf<KClass<Entity>, Resource<Entity>>()
        for (entityType in entities) {
            resourcesBuilder[entityType as KClass<Entity>] = Resource(root, entityType, objectMapper)
        }
        resources = resourcesBuilder.toMap()
    }

    inline fun <reified E : Entity> get(vararg ids: Pair<KProperty1<E, UUID>, UUID>): E {
        val resource: Resource<E> = resources[E::class as KClass<Entity>]!!  as Resource<E>
        return resource.get(*ids)
    }

    inline fun <reified E : Entity> put(entity: E) {
        val resource = resources[E::class as KClass<Entity>]!! as Resource<E>
        resource.put(entity)
    }

    inline fun <reified E : Entity> remove(entity: E) {
        val resource = resources[E::class as KClass<Entity>]!! as Resource<E>
        resource.delete(entity)
    }

    inline fun <reified E: Entity> list(vararg ids: Pair<KProperty1<E, UUID>, UUID>): List<E> {
        val resource: Resource<E> = resources[E::class as KClass<Entity>]!!  as Resource<E>
        return resource.list(*ids)
    }
}
