package org.coner.snoozle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import kotlin.reflect.KClass

class Database(
        root: File,
        vararg entities: KClass<*>,
        objectMapper: ObjectMapper = jacksonObjectMapper()
) {

    val resources: Map<KClass<*>, Resource<*>>

    init {
        val resourcesBuilder = mutableMapOf<KClass<*>, Resource<*>>()
        for (entityType in entities) {
            resourcesBuilder[entityType] = Resource(root, entityType, objectMapper)
        }
        resources = resourcesBuilder.toMap()
    }

    inline fun <reified E : Any> getById(id: String): E {
        val resource = resources[E::class]!!
        return resource.get(id)
    }
}
