package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import de.helmbold.rxfilewatcher.PathObservables
import io.reactivex.Observable
import org.coner.snoozle.util.uuid
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.streams.toList

class Resource<E : Entity>(
        val root: File,
        val entityDefinition: EntityDefinition<E>,
        val objectMapper: ObjectMapper,
        val path: Pathfinder<E> = Pathfinder(entityDefinition.kClass),
        private val entityIoDelegate: EntityIoDelegate<E> = EntityIoDelegate(
                objectMapper,
                objectMapper.readerFor(entityDefinition.kClass.java),
                objectMapper.writerFor(entityDefinition.kClass.java)
        ),
        automaticEntityVersionIoDelegate: AutomaticEntityVersionIoDelegate<E>? = null
) {

    private val automaticEntityVersionIoDelegate: AutomaticEntityVersionIoDelegate<E>?

    init {
        val useAutomaticEntityVersionIoDelegate = entityDefinition.kClass
                .findAnnotation<AutomaticVersionedEntity>() != null
        this.automaticEntityVersionIoDelegate = if (automaticEntityVersionIoDelegate != null) {
            automaticEntityVersionIoDelegate
        } else if (useAutomaticEntityVersionIoDelegate) {
            AutomaticEntityVersionIoDelegate(objectMapper, entityIoDelegate)
        } else {
            null
        }
    }


    fun get(vararg ids: Pair<KProperty1<E, UUID>, UUID>): E {
        val entityPath = path.findEntity(*ids)
        val file = File(root, entityPath)
        if (!file.exists()) {
            throw EntityIoException("Entity does not exist: $entityPath")
        }
        val rootNode = objectMapper.readTree(file) as ObjectNode
        return entityIoDelegate.read(rootNode)
    }

    fun put(entity: E) {
        val entityParentPath = path.findParentOfEntity(entity)
        val parent = File(root, entityParentPath)
        if (!parent.exists()) {
            if (!parent.mkdir()) {
                throw EntityIoException("""
                    Failed to create parent folder:
                    $entityParentPath
                    Does its parent exist?
                """.trimIndent())
            }
        }
        val file = File(root, path.findEntity(entity))
        val oldRoot = if (file.exists())
            objectMapper.readTree(file) as ObjectNode
        else
            null
        val newRoot = objectMapper.createObjectNode()
        entityIoDelegate.write(
                oldRoot = oldRoot,
                newRoot = newRoot,
                newContent = entity
        )
        automaticEntityVersionIoDelegate?.write(
                oldRoot = oldRoot,
                newRoot = newRoot,
                newContent = entity
        )
        objectMapper.writeValue(file, newRoot)
    }

    fun delete(entity: E) {
        val file = File(root, path.findEntity(entity))
        file.delete()
    }

    fun list(vararg ids: Pair<KProperty1<E, UUID>, UUID>): List<E> {
        val listingPath = path.findListing(*ids)
        val listing = File(root, listingPath)
        if (!listing.exists()) {
            if (!listing.mkdirs())
                throw EntityIoException("""
                    Failed to create listing:
                    $listingPath
                    Does its parent exist?
                """.trimIndent())
        }
        return listing.listFiles()
                .filter { it.isFile && it.extension == "json" }
                .parallelStream()
                .sorted(compareBy(File::getName))
                .map { file ->
                    val rootNode = objectMapper.readTree(file) as ObjectNode
                    entityIoDelegate.read(rootNode)
                }
                .toList()
    }

    fun watchListing(vararg ids: Pair<KProperty1<E, UUID>, UUID>): Observable<EntityEvent<E>> {
        val listing = File(root, path.findListing(*ids))
        return PathObservables.watchNonRecursive(listing.toPath())
                .filter { path.isValidEntity((it.context() as Path).toFile()) }
                .map {
                    val file = File(listing, (it.context() as Path).toFile().name)
                    val entity = if (file.exists() && file.length() > 0) {
                        val rootNode = objectMapper.readTree(file) as ObjectNode
                        entityIoDelegate.read(rootNode)
                    } else {
                        null
                    }
                    EntityEvent(it, uuid(file.nameWithoutExtension), entity)
                }
    }
}