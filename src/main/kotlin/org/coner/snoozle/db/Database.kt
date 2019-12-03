package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.util.snoozleJacksonObjectMapper
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class Database(
        protected val root: Path,
        protected val objectMapper: ObjectMapper = snoozleJacksonObjectMapper()
) {
    abstract val entities: EntitiesManifest

    protected fun registerEntity(op: EntitiesManifest.(Map<KClass<*>, EntityResource<*>>) -> Unit): EntitiesManifest {
        return EntitiesManifest(root, op, objectMapper)
    }

    inline fun <reified E : Entity> entity(): EntityResource<E> {
        return entities.entityResources[E::class] as EntityResource<E>
    }


}

