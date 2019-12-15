package org.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectMapper
import de.helmbold.rxfilewatcher.PathObservables
import io.reactivex.Observable
import org.coner.snoozle.db.path.Pathfinder
import org.coner.snoozle.util.nameWithoutExtension
import org.coner.snoozle.util.uuid
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardWatchEventKinds
import kotlin.streams.toList

class EntityResource<E : Entity> constructor(
        private val root: Path,
        internal val entityDefinition: EntityDefinition<E>,
        private val objectMapper: ObjectMapper,
        private val path: Pathfinder<E>,
        private val entityIoDelegate: EntityIoDelegate<E>,
        private val automaticEntityVersionIoDelegate: AutomaticEntityVersionIoDelegate<E>?
) {

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
            val message = "Failed to create parent folder for ${entityDefinition::class.java.simpleName} $entityParentPath"
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

    private fun write(destination: Path, entity: E) {
        val old = try {
            read(destination)
        } catch (entityIoException: EntityIoException.NotFound) {
            null
        }
        val new = WholeRecord.Builder<E>()
        entityIoDelegate.write(old, new, entity)
        automaticEntityVersionIoDelegate?.write(old, new, entity)
        val tempFile = createTempFile().toPath()
        Files.newOutputStream(tempFile).use { outputStream ->
            objectMapper.writeValue(outputStream, new)
        }
        Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
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

    fun watchListing(vararg args: Any): Observable<EntityEvent<E>> {
        val relativeListing = path.findListingByArgs(*args)
        val absoluteListing = root.resolve(relativeListing)
        return PathObservables.watchNonRecursive(absoluteListing)
                .filter { path.isRecord(relativeListing.resolve(it.context() as Path)) }
                .map {
                    val file = absoluteListing.resolve(it.context() as Path)
                        val entity = if (it.kind() != StandardWatchEventKinds.ENTRY_DELETE
                                && Files.exists(file)
                                && Files.size(file) > 0
                        ) {
                            try {
                                read(file).entityValue
                            } catch (t: Throwable) {
                                null
                            }
                        } else {
                            null
                        }
                        it to entity
                }
                .filter {
                    when (it.first.kind()) {
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE -> it.second != null
                        StandardWatchEventKinds.ENTRY_DELETE -> it.second == null
                        StandardWatchEventKinds.OVERFLOW -> true
                        else -> false
                    }
                }
                .map {
                    val state = when(it.first.kind()) {
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE -> EntityEvent.State.EXISTS
                        StandardWatchEventKinds.ENTRY_DELETE -> EntityEvent.State.DELETED
                        StandardWatchEventKinds.OVERFLOW -> EntityEvent.State.OVERFLOW
                        else -> throw IllegalStateException("Other event types filtered already")
                    }
                    EntityEvent(
                            state = state,
                            id = uuid((it.first.context() as Path).nameWithoutExtension),
                            entity = it.second
                    )
                }
    }
}