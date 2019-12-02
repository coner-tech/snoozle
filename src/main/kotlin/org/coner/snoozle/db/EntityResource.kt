package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import de.helmbold.rxfilewatcher.PathObservables
import io.reactivex.Observable
import org.coner.snoozle.util.extension
import org.coner.snoozle.util.nameWithoutExtension
import org.coner.snoozle.util.uuid
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.streams.toList

class EntityResource<E : Entity> internal constructor(
        private val root: Path,
        internal val entityDefinition: EntityDefinition<E>,
        private val objectMapper: ObjectMapper,
        entityIoDelegate: EntityIoDelegate<E>? = null,
        automaticEntityVersionIoDelegate: AutomaticEntityVersionIoDelegate<E>? = null
) {

    constructor(
            root: Path,
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

    val pathfinder: Pathfinder<E>
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
            useAutomaticEntityVersionIoDelegate -> AutomaticEntityVersionIoDelegate(
                    reader = objectMapper.readerFor(entityDefinition.kClass.java),
                    entityDefinition = entityDefinition
            )
            else -> null
        }
        this.pathfinder = when {
            path != null -> path
            else -> Pathfinder(entityDefinition.kClass)
        }
    }

    fun get(vararg ids: Pair<KProperty1<E, UUID>, UUID>): E {
        return getWholeRecord(*ids).entityValue
    }

    fun getWholeRecord(vararg ids: Pair<KProperty1<E, UUID>, UUID>): WholeRecord<E> {
        val entityPath = pathfinder.findEntity(*ids)
        val file = root.resolve(entityPath)
        return read(file)
    }

    fun put(entity: E) {
        val entityParentPath = pathfinder.findParentOfEntity(entity)
        val parent = root.resolve(entityParentPath)
        try {
            Files.createDirectory(parent)
        } catch (fileAlreadyExists: FileAlreadyExistsException) {
            // that's fine
        } catch (t: Throwable) {
            val message = """
                Failed to create parent folder:
                $entityParentPath
                Does its parent exist?
            """.trimIndent()
            throw EntityIoException.WriteFailure(message, t)
        }
        val file = root.resolve(pathfinder.findEntity(entity))
        write(file, entity)
    }

    private fun read(file: Path): WholeRecord<E> {
        return if (Files.exists(file)) {
            Files.newInputStream(file).use { inputStream ->
                try {
                    val builder = (objectMapper.readValue(inputStream, WholeRecord.Builder::class.java) as WholeRecord.Builder<E>)
                    entityIoDelegate.read(builder)
                    automaticEntityVersionIoDelegate?.read(builder)
                    builder.build()
                } catch (t: Throwable) {
                    throw EntityIoException.ReadFailure("Failed to read/build entity: ${file.relativize(root)}", t)
                }
            }
        } else {
            throw EntityIoException.NotFound("Entity not found: ${root.relativize(file)}")
        }
    }

    private fun write(file: Path, entity: E) {
        val old = try {
            read(file)
        } catch (entityIoException: EntityIoException.NotFound) {
            null
        }
        val new = WholeRecord.Builder<E>()
        entityIoDelegate.write(old, new, entity)
        automaticEntityVersionIoDelegate?.write(old, new, entity)
        Files.newOutputStream(file).use { outputStream ->
            objectMapper.writeValue(outputStream, new)
        }
    }

    fun delete(entity: E) {
        val file = root.resolve(pathfinder.findEntity(entity))
        Files.delete(file)
    }

    fun list(vararg ids: Pair<KProperty1<E, UUID>, UUID>): List<E> {
        val listingPath = pathfinder.findListing(*ids)
        val listing = root.resolve(listingPath)
        if (!Files.exists(listing)) {
            try {
                Files.createDirectories(listing)
            } catch (t: Throwable) {
                val message = """
                    Failed to create listing:
                    $listingPath
                    Does its parent exist?
                """.trimIndent()
                throw EntityIoException.WriteFailure(message, t)
            }
        }
        return Files.list(listing)
                .filter { Files.isRegularFile(it) && it.extension == "json" }
                .sorted(compareBy(Path::toString))
                .map { file -> read(file).entityValue }
                .toList()
    }

    fun watchListing(vararg ids: Pair<KProperty1<E, UUID>, UUID>): Observable<EntityEvent<E>> {
        val listing = root.resolve(pathfinder.findListing(*ids))
        return PathObservables.watchNonRecursive(listing)
                .filter { pathfinder.isValidEntity((it.context() as Path)) }
                .map {
                    val file = listing.resolve(it.context() as Path)
                    val entity = if (Files.exists(file) && Files.size(file) > 0) {
                        read(file).entityValue
                    } else {
                        null
                    }
                    EntityEvent(it, uuid(file.nameWithoutExtension), entity)
                }
    }
}