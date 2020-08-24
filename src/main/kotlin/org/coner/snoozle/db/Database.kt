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

    inline fun <reified E : Entity<K>, K : Key> entity(): EntityResource<E, K> {
        return entity(E::class)
    }

    fun <E : Entity<K>, K : Key> entity(type: KClass<E>): EntityResource<E, K> {
        return types.entityResources[type] as EntityResource<E, K>
    }

    inline fun <reified VE : VersionedEntity<K>, K : Key> versionedEntity(): VersionedEntityResource<VE, K> {
        return versionedEntity(VE::class)
    }

    fun <VE : VersionedEntity<K>, K : Key> versionedEntity(entity: KClass<VE>): VersionedEntityResource<VE, K> {
        return types.versionedEntityResources[entity]
    }

    inline fun <reified B : Blob> blob(): BlobResource<B> {
        return blob(B::class)
    }

    fun <B : Blob> blob(type: KClass<B>): BlobResource<B> {
        return types.blobResources[type] as BlobResource<B>
    }

}

