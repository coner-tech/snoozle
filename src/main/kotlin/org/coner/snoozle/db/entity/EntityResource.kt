package org.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import io.reactivex.Observable
import org.coner.snoozle.db.Key
import org.coner.snoozle.db.Pathfinder
import org.coner.snoozle.util.nameWithoutExtension
import org.coner.snoozle.util.uuid
import org.coner.snoozle.util.watch
import java.nio.file.*
import java.util.function.Predicate
import java.util.stream.Stream

class EntityResource<E : Entity<K>, K : Key> constructor(
        private val root: Path,
        internal val definition: EntityDefinition<E, K>,
        private val reader: ObjectReader,
        private val writer: ObjectWriter,
        private val path: Pathfinder<E, K>,
        private val key: EntityKeyParser<E, K>
) {

    fun get(key: K): E {
        val entityPath = path.findRecord(key)
        val file = root.resolve(entityPath)
        return read(file)
    }

    fun put(entity: E) {
        val entityParentPath = path.findListingByRecord(entity)
        val parent = root.resolve(entityParentPath)
        if (!Files.exists(parent)) {
            try {
                Files.createDirectories(parent)
            } catch (t: Throwable) {
                val message = "Failed to create parent folder for ${definition::class.java.simpleName} $entityParentPath"
                throw EntityIoException.WriteFailure(message, t)
            }
        }
        val file = root.resolve(path.findRecord(entity))
        write(file, entity)
    }

    private fun read(file: Path): E {
        return if (Files.exists(file)) {
            Files.newInputStream(file).use { inputStream ->
                try {
                    reader.readValue<E>(inputStream)
                } catch (t: Throwable) {
                    throw EntityIoException.ReadFailure("Failed to read entity: ${file.relativize(root)}", t)
                }
            }
        } else {
            throw EntityIoException.NotFound("Entity not found: ${root.relativize(file)}")
        }
    }

    private fun write(destination: Path, entity: E) {
        val tempFile = destination.resolveSibling(destination.fileName.toString() + ".tmp")
        Files.newOutputStream(tempFile, StandardOpenOption.CREATE_NEW).use { outputStream ->
            writer.writeValue(outputStream, entity)
        }
        Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun delete(entity: E) {
        val file = root.resolve(path.findRecord(entity))
        Files.delete(file)
    }

    fun streamAll(keyFilter: Predicate<K>? = null): Stream<E> {
        val allPathsMappedToKeys = path.streamAll()
                .map { recordPath: Path -> key.parse(recordPath) }
        val allKeysForRead = keyFilter?.let { allPathsMappedToKeys.filter(it) } ?: allPathsMappedToKeys
        return allKeysForRead.map { read(path.findRecord(it)) }
    }

    fun watchListing(): Observable<EntityEvent<E, K>> {
        return root.watch(recursive = true)
                .filter { path.isRecord(root.relativize(it.context() as Path)) }
                .map {
                    val file = it.context() as Path
                        val entity = if (it.kind() != StandardWatchEventKinds.ENTRY_DELETE
                                && Files.exists(file)
                                && Files.size(file) > 0
                        ) {
                            try {
                                read(file)
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