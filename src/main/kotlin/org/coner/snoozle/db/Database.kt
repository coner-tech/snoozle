package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.db.blob.Blob
import org.coner.snoozle.db.blob.BlobResource
import org.coner.snoozle.db.entity.*
import org.coner.snoozle.util.snoozleJacksonObjectMapper
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class Database(
        protected val root: Path,
        private val objectMapper: ObjectMapper = snoozleJacksonObjectMapper()
) {
    protected abstract val types: TypesRegistry

    protected fun registerTypes(op: TypesRegistry.() -> Unit): TypesRegistry {
        return TypesRegistry(root, objectMapper, op)
    }

    inline fun <K : Key, reified E : Entity<K>> entity(): EntityResource<K, E> {
        return entity(E::class)
    }

    fun <K : Key, E : Entity<K>> entity(type: KClass<E>): EntityResource<K, E> {
        return types.entityResources[type] as EntityResource<K, E>
    }

    inline fun <reified B : Blob> blob(): BlobResource<B> {
        return blob(B::class)
    }

    fun <B : Blob> blob(type: KClass<B>): BlobResource<B> {
        return types.blobResources[type] as BlobResource<B>
    }

}

