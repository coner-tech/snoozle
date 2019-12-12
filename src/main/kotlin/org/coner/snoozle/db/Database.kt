package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.util.snoozleJacksonObjectMapper
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class Database(
        protected val root: Path,
        private val objectMapper: ObjectMapper = snoozleJacksonObjectMapper()
) {
    protected abstract val entities: EntitiesManifest

    protected fun registerEntities(op: EntitiesManifest.(Map<KClass<*>, EntityResource<*>>) -> Unit): EntitiesManifest {
        return EntitiesManifest(root, op, objectMapper)
    }

    inline fun <reified E : Entity> entity(): EntityResource<E> {
        return entity(E::class)
    }

    fun <E : Entity> entity(type: KClass<E>): EntityResource<E> {
        return entities.entityResources[type] as EntityResource<E>
    }

}

