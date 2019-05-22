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

class Resource<E : Entity> internal constructor(
        private val root: File,
        internal val entityDefinition: EntityDefinition<E>,
        private val objectMapper: ObjectMapper,
        path: Pathfinder<E>? = null,
        entityIoDelegate: EntityIoDelegate<E>? = null,
        automaticEntityVersionIoDelegate: AutomaticEntityVersionIoDelegate<E>? = null
) {

    constructor(
            root: File,
            entityDefinition: EntityDefinition<E>,
            objectMapper: ObjectMapper
    ) : this(
        root = root,
        entityDefinition = entityDefinition,
        objectMapper = objectMapper,
        path = null,
        entityIoDelegate = null,
        automaticEntityVersionIoDelegate = null
    )

    val path: Pathfinder<E>
    private val automaticEntityVersionIoDelegate: AutomaticEntityVersionIoDelegate<E>?
    private val entityIoDelegate: EntityIoDelegate<E> = when {
        entityIoDelegate != null -> entityIoDelegate
        else -> EntityIoDelegate(
                objectMapper,
                objectMapper.readerFor(entityDefinition.kClass.java),
                objectMapper.writerFor(entityDefinition.kClass.java)
        )
    }

    init {
        val useAutomaticEntityVersionIoDelegate = entityDefinition.kClass
                .findAnnotation<AutomaticVersionedEntity>() != null
        this.automaticEntityVersionIoDelegate = when {
            automaticEntityVersionIoDelegate != null -> automaticEntityVersionIoDelegate
            useAutomaticEntityVersionIoDelegate -> AutomaticEntityVersionIoDelegate(objectMapper, this.entityIoDelegate)
            else -> null
        }
        this.path = when {
            path != null -> path
            else -> Pathfinder(entityDefinition.kClass)
        }
    }

    fun get(vararg ids: Pair<KProperty1<E, UUID>, UUID>): E {
        val entityPath = path.findEntity(*ids)
        val file = File(root, entityPath)
        return read(file).entityValue
    }

    fun put(entity: E) {
        val entityParentPath = path.findParentOfEntity(entity)
        val parent = File(root, entityParentPath)
        if (!parent.exists()) {
            if (!parent.mkdir()) {
                throw EntityIoException.WriteFailure("""
                    Failed to create parent folder:
                    $entityParentPath
                    Does its parent exist?
                """.trimIndent())
            }
        }
        val file = File(root, path.findEntity(entity))
        write(file, entity)
    }

    private fun read(file: File): WholeRecord<E> {
        return if (file.exists()) {
            try {
                val builder = (objectMapper.readValue(file, WholeRecord.Builder::class.java) as WholeRecord.Builder<E>)
                entityIoDelegate.read(builder)
                automaticEntityVersionIoDelegate?.read(builder)
                builder.build()
            } catch (t: Throwable) {
                throw EntityIoException.ReadFailure("Failed to read/build entity: ${file.relativeTo(root)}", t)
            }
        } else {
            throw EntityIoException.NotFound("Entity not found: ${file.relativeTo(root)}")
        }
    }

    private fun write(file: File, entity: E) {
        val old = try {
            read(file)
        } catch (entityIoException: EntityIoException.NotFound) {
            null
        }
        val new = WholeRecord.Builder<E>()
        entityIoDelegate.write(old, new, entity)
        automaticEntityVersionIoDelegate?.write(old, new, entity)
        objectMapper.writeValue(file, new)
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
                throw EntityIoException.WriteFailure("""
                    Failed to create listing:
                    $listingPath
                    Does its parent exist?
                """.trimIndent())
        }
        return listing.listFiles()
                .filter { it.isFile && it.extension == "json" }
                .parallelStream()
                .sorted(compareBy(File::getName))
                .map { file -> read(file).entityValue }
                .toList()
    }

    fun watchListing(vararg ids: Pair<KProperty1<E, UUID>, UUID>): Observable<EntityEvent<E>> {
        val listing = File(root, path.findListing(*ids))
        return PathObservables.watchNonRecursive(listing.toPath())
                .filter { path.isValidEntity((it.context() as Path).toFile()) }
                .map {
                    val file = File(listing, (it.context() as Path).toFile().name)
                    val entity = if (file.exists() && file.length() > 0) {
                        read(file).entityValue
                    } else {
                        null
                    }
                    EntityEvent(it, uuid(file.nameWithoutExtension), entity)
                }
    }
}