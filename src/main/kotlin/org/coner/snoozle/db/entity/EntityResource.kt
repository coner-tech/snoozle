package org.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import io.reactivex.Observable
import org.coner.snoozle.db.Key
import org.coner.snoozle.db.KeyMapper
import org.coner.snoozle.db.Pathfinder
import org.coner.snoozle.util.watch
import java.nio.file.*
import java.util.function.Predicate
import java.util.stream.Stream

class EntityResource<K : Key, E : Entity<K>> constructor(
        private val root: Path,
        internal val definition: EntityDefinition<K, E>,
        private val reader: ObjectReader,
        private val writer: ObjectWriter,
        private val pathfinder: Pathfinder<K, E>,
        private val keyMapper: KeyMapper<K, E>
) {

    fun get(key: K): E {
        val entityPath = pathfinder.findRecord(key)
        val file = root.resolve(entityPath)
        return read(file)
    }

    fun put(entity: E) {
        val key = keyMapper.fromInstance(entity)
        val relativeRecord = pathfinder.findRecord(key)
        val absoluteParent = root.resolve(relativeRecord.parent)
        if (!Files.exists(absoluteParent)) {
            try {
                Files.createDirectories(absoluteParent)
            } catch (t: Throwable) {
                val message = "Failed to create parent folder for ${definition.recordClass.simpleName} ${relativeRecord.parent}"
                throw EntityIoException.WriteFailure(message, t)
            }
        }
        val file = root.resolve(relativeRecord)
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

    fun delete(key: K) {
        val file = root.resolve(pathfinder.findRecord(key))
        Files.delete(file)
    }

    fun stream(keyFilter: Predicate<K>? = null): Stream<E> {
        val allPathsMappedToKeys = pathfinder.streamAll()
                .map { recordPath: Path -> keyMapper.fromRelativeRecord(recordPath) }
        val allKeysForRead = keyFilter?.let { allPathsMappedToKeys.filter(it) } ?: allPathsMappedToKeys
        return allKeysForRead.map { read(pathfinder.findRecord(it)) }
    }

    fun watch(keyFilter: Predicate<K>? = null): Observable<EntityEvent<K, E>> {
        return root.watch(recursive = true)
                .map { event: WatchEvent<*> ->
                    val file = event.context() as Path
                    PreReadWatchEventPayload(
                            event = event,
                            file = file,
                            key = keyMapper.fromRelativeRecord(root.relativize(file))
                    )
                }
                .filter { pathfinder.isRecord(it.file) && keyFilter?.test(it.key) != false }
                .map {
                    val entity = if (it.event.kind() != StandardWatchEventKinds.ENTRY_DELETE
                            && Files.exists(it.file)
                            && Files.size(it.file) > 0
                    ) {
                        try {
                            read(it.file)
                        } catch (t: Throwable) {
                            null
                        }
                    } else {
                        null
                    }
                    PostReadWatchEventPayload(it.event, it.key, entity)
                }
                .filter {
                    when (it.event.kind()) {
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE -> it.entity != null
                        StandardWatchEventKinds.ENTRY_DELETE -> it.entity == null
                        StandardWatchEventKinds.OVERFLOW -> true
                        else -> false
                    }
                }
                .map {
                    val state = when(it.event.kind()) {
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE -> EntityEvent.State.EXISTS
                        StandardWatchEventKinds.ENTRY_DELETE -> EntityEvent.State.DELETED
                        StandardWatchEventKinds.OVERFLOW -> EntityEvent.State.OVERFLOW
                        else -> throw IllegalStateException("Other event types filtered already")
                    }
                    EntityEvent(
                            state = state,
                            key = it.key,
                            entity = it.entity
                    )
                }
    }

    private class PreReadWatchEventPayload<K : Key>(
        val event: WatchEvent<*>,
        val file: Path,
        val key: K
    )

    private class PostReadWatchEventPayload<K : Key, E : Entity<K>>(
            val event: WatchEvent<*>,
            val key: K,
            val entity: E?
    )
}