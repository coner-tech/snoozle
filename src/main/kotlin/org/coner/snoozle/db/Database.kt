package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.db.blob.Blob
import org.coner.snoozle.db.blob.BlobResource
import org.coner.snoozle.db.entity.Entity
import org.coner.snoozle.db.entity.EntityResource
import org.coner.snoozle.db.entity.VersionedEntity
import org.coner.snoozle.db.entity.VersionedEntityResource
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

    inline fun <reified E : Entity> entity(): EntityResource<E> {
        return entity(E::class)
    }

    fun <E : Entity> entity(type: KClass<E>): EntityResource<E> {
        return types.entityResources[type] as EntityResource<E>
    }

    inline fun <reified VE : VersionedEntity> versionedEntity(): VersionedEntityResource<VE> {
        return versionedEntity(VE::class)
    }

    fun <VE : VersionedEntity> versionedEntity(type: KClass<VE>): VersionedEntityResource<VE> {
        return types.versionedEntityResources[type] as VersionedEntityResource<VE>
    }

    inline fun <reified B : Blob> blob(): BlobResource<B> {
        return blob(B::class)
    }

    fun <B : Blob> blob(type: KClass<B>): BlobResource<B> {
        return types.blobResources[type] as BlobResource<B>
    }

}

