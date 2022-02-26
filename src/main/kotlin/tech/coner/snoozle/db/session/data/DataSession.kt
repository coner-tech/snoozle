package tech.coner.snoozle.db.session.data

import tech.coner.snoozle.db.Key
import tech.coner.snoozle.db.TypesRegistry
import tech.coner.snoozle.db.blob.Blob
import tech.coner.snoozle.db.blob.BlobResource
import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.metadata.MetadataRepository
import tech.coner.snoozle.db.session.Session
import java.util.*
import kotlin.reflect.KClass

class DataSession(
    private val types: TypesRegistry,
    metadataRepository: MetadataRepository,
    onClose: () -> Unit
) : Session(
    id = UUID.randomUUID(),
    metadataRepository = metadataRepository,
    onClose = onClose
) {

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