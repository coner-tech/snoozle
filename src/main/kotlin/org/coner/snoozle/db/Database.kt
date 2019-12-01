package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.util.snoozleJacksonObjectMapper
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class Database(
        protected val root: Path,
        protected val objectMapper: ObjectMapper = snoozleJacksonObjectMapper()
) {
    protected abstract val entities: EntitiesManifest

    protected fun entities(op: EntitiesManifest.(Map<KClass<*>, EntityResource<*>>) -> Unit): EntitiesManifest {
        return EntitiesManifest(op)
    }
}

