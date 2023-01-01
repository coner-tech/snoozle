package tech.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import tech.coner.snoozle.db.blob.Blob
import tech.coner.snoozle.db.blob.BlobDefinition
import tech.coner.snoozle.db.blob.BlobResource
import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.entity.EntityDefinition
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.metadata.DatabaseVersionBlob
import tech.coner.snoozle.db.metadata.SessionMetadataEntity
import kotlin.reflect.KClass

class TypesRegistry(
    val root: AbsolutePath,
    val fileWatchEngine: FileWatchEngine,
    val objectMapper: ObjectMapper
) {
    val entityResources = mutableMapOf<KClass<*>, EntityResource<*, *>>()
    val blobResources = mutableMapOf<KClass<*>, BlobResource<*>>()

    init {
        blob<DatabaseVersionBlob> {
            path = listOf(
                PathPart.StringValue(".snoozle"),
                PathPart.DirectorySeparator(),
                PathPart.StringValue("database_version")
            )
            keyFromPath = { DatabaseVersionBlob }
        }
        entity<SessionMetadataEntity.Key, SessionMetadataEntity> {
            path = listOf(
                PathPart.StringValue(".snoozle"),
                PathPart.DirectorySeparator(),
                PathPart.StringValue("sessions"),
                PathPart.DirectorySeparator(),
                PathPart.UuidVariable { id },
                PathPart.StringValue(".json")
            )
            keyFromPath = { SessionMetadataEntity.Key(id = uuidAt(0)) }
            keyFromEntity = { SessionMetadataEntity.Key(id = id) }
        }
    }

    inline fun <reified K : Key, reified E : Entity<K>> entity(op: EntityDefinition<K, E>.() -> Unit) {
        val entityDefinition = EntityDefinition(
                keyClass = K::class,
                entityClass = E::class
        ).apply(op)
        val pathfinder = Pathfinder(
                root = root,
                pathParts = entityDefinition.path
        )
        entityResources[E::class] = EntityResource(
            root = root,
            definition = entityDefinition,
            reader = objectMapper.readerFor(E::class.java),
            writer = objectMapper.writerFor(E::class.java),
            pathfinder = pathfinder,
            keyMapper = KeyMapper(
                definition = entityDefinition,
                pathfinder = pathfinder,
                relativeRecordFn = requireNotNull(entityDefinition.keyFromPath),
                instanceFn = requireNotNull(entityDefinition.keyFromEntity)
            ),
            fileWatchEngine = fileWatchEngine
        )
    }

    inline fun <reified B : Blob> blob(op: BlobDefinition<B>.() -> Unit) {
        val definition = BlobDefinition(
                blobClass = B::class
        ).apply(op)
        val pathfinder = Pathfinder(
                root = root,
                pathParts = definition.path
        )
        blobResources[B::class] = BlobResource(
                root = root,
                definition =  definition,
                pathfinder = pathfinder,
                keyMapper = KeyMapper(
                    definition = definition,
                    pathfinder = pathfinder,
                    relativeRecordFn = requireNotNull(definition.keyFromPath),
                    instanceFn = { this as B }
                )
        )
    }
}