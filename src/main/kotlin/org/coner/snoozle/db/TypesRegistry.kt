package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.db.blob.Blob
import org.coner.snoozle.db.blob.BlobDefinition
import org.coner.snoozle.db.blob.BlobResource
import org.coner.snoozle.db.entity.*
import org.coner.snoozle.db.path.Pathfinder
import java.nio.file.Path
import kotlin.reflect.KClass

class TypesRegistry(
        val root: Path,
        val objectMapper: ObjectMapper,
        op: TypesRegistry.() -> Unit
) {
    val entityResources = mutableMapOf<KClass<*>, EntityResource<*>>()
    val versionedEntityResources = mutableMapOf<KClass<*>, VersionedEntityResource<*>>()
    val blobResources = mutableMapOf<KClass<*>, BlobResource<*>>()

    init {
        this.op()
    }

    inline fun <reified E : Entity> entity(op: EntityDefinition<E>.() -> Unit) {
        val entityDefinition = EntityDefinition<E>().apply(op)
        entityResources[E::class] = EntityResource(
                root = root,
                entityDefinition = entityDefinition,
                objectMapper = objectMapper,
                reader = objectMapper.readerFor(E::class.java),
                writer = objectMapper.writerFor(E::class.java),
                path = Pathfinder(entityDefinition.path)
        )
    }

    inline fun <reified E : VersionedEntity> versionedEntity(op: VersionedEntityDefinition<E>.() -> Unit) {
        val versionedEntityDefinition = VersionedEntityDefinition<E>().apply(op)
        versionedEntityResources[E::class] = VersionedEntityResource<E>(
                root = root,
                versionedEntityDefinition = versionedEntityDefinition,
                objectMapper = objectMapper,
                reader = objectMapper.readerFor(E::class.java),
                writer = objectMapper.writerFor(E::class.java),
                path = Pathfinder(versionedEntityDefinition.path)
        )
    }

    inline fun <reified B : Blob> blob(op: BlobDefinition<B>.() -> Unit) {
        val blobDefinition = BlobDefinition<B>().apply(op)
        blobResources[B::class] = BlobResource(
                root = root,
                definition =  blobDefinition,
                path = Pathfinder(blobDefinition.path)
        )
    }
}