package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Observable
import org.coner.snoozle.db.path.Pathfinder
import org.coner.snoozle.util.extension
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.streams.toList

class EntityResource<E : Entity> constructor(
        private val root: Path,
        internal val entityDefinition: EntityDefinition<E>,
        private val objectMapper: ObjectMapper,
        private val path: Pathfinder<E>,
        private val entityIoDelegate: EntityIoDelegate<E>,
        private val automaticEntityVersionIoDelegate: AutomaticEntityVersionIoDelegate<E>?
) {

//    val pathfinder: Pathfinder<E>
//    private val automaticEntityVersionIoDelegate: AutomaticEntityVersionIoDelegate<E>?
//    private val entityIoDelegate: EntityIoDelegate<E> = when {
//        entityIoDelegate != null -> entityIoDelegate
//        else -> EntityIoDelegate(
//                objectMapper,
//                objectMapper.readerFor(entityDefinition.kClass.java),
//                objectMapper.writerFor(entityDefinition.kClass.java)
//        )
//    }

    init {
//        val useAutomaticEntityVersionIoDelegate = entityDefinition.kClass
//                .findAnnotation<AutomaticVersionedEntity>() != null
//        this.automaticEntityVersionIoDelegate = when {
//            automaticEntityVersionIoDelegate != null -> automaticEntityVersionIoDelegate
//            useAutomaticEntityVersionIoDelegate -> AutomaticEntityVersionIoDelegate(
//                    reader = objectMapper.readerFor(entityDefinition.kClass.java),
//                    entityDefinition = entityDefinition
//            )
//            else -> null
//        }
//        this.pathfinder = when {
//            path != null -> path
//            else -> Pathfinder(entityDefinition.kClass)
//        }
    }

    fun get(vararg args: Any): E {
        return getWholeRecord(*args).entityValue
    }

    fun getWholeRecord(vararg args: Any): WholeRecord<E> {
        val entityPath = path.findRecordByArgs(*args)
        val file = root.resolve(entityPath)
        return read(file)
    }

    fun put(entity: E) {
        val entityParentPath = path.findListingByRecord(entity)
        val parent = root.resolve(entityParentPath)
        try {
            Files.createDirectories(parent)
        } catch (fileAlreadyExists: FileAlreadyExistsException) {
            // that's fine
        } catch (t: Throwable) {
            val message = "Failed to create parent folder for ${entityDefinition::class.java.simpleName} $parent"
            throw EntityIoException.WriteFailure(message, t)
        }
        val file = root.resolve(path.findRecord(entity))
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
        val file = root.resolve(path.findRecord(entity))
        Files.delete(file)
    }

    fun list(vararg args: Any): List<E> {
        val listingPath = path.findListingByArgs(*args)
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
                .filter { Files.isRegularFile(it) && path.isRecord(root.relativize(it)) }
                .sorted(compareBy(Path::toString))
                .map { file -> read(file).entityValue }
                .toList()
    }

    fun watchListing(vararg ids: Pair<KProperty1<E, UUID>, UUID>): Observable<EntityEvent<E>> {
        TODO()
//        val listing = root.resolve(pathfinder.findListing(*ids))
//        return PathObservables.watchNonRecursive(listing)
//                .filter { pathfinder.isValidEntity((it.context() as Path)) }
//                .map {
//                    val file = listing.resolve(it.context() as Path)
//                    val entity = if (Files.exists(file) && Files.size(file) > 0) {
//                        read(file).entityValue
//                    } else {
//                        null
//                    }
//                    EntityEvent(it, uuid(file.nameWithoutExtension), entity)
//                }
    }
}