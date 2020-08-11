package org.coner.snoozle.db

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JavaType
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

    inline fun <reified VE : VersionedEntity> versionedEntity(op: VersionedEntityDefinition<VE>.() -> Unit) {
        val versionedEntityDefinition = VersionedEntityDefinition<VE>().apply(op)
        val jacksonTypeReference = object : TypeReference<VersionedEntityContainer<VE>>() { }
        versionedEntityResources[VE::class] = VersionedEntityResource(
                root = root,
                versionedEntityDefinition = versionedEntityDefinition,
                objectMapper = objectMapper,
                reader = objectMapper.readerFor(jacksonTypeReference),
                writer = objectMapper.writerFor(jacksonTypeReference),
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