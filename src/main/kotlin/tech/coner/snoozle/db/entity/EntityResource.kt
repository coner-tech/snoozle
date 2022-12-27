package tech.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import io.reactivex.Observable
import tech.coner.snoozle.db.Key
import tech.coner.snoozle.db.KeyMapper
import tech.coner.snoozle.db.Pathfinder
import tech.coner.snoozle.db.WatchEngine
import tech.coner.snoozle.util.PathWatchEvent
import tech.coner.snoozle.util.watch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.StandardWatchEventKinds
import java.util.function.Predicate
import java.util.stream.Stream
import kotlin.io.path.notExists

class EntityResource<K : Key, E : Entity<K>> constructor(
    private val root: Path,
    internal val definition: EntityDefinition<K, E>,
    private val reader: ObjectReader,
    private val writer: ObjectWriter,
    private val pathfinder: Pathfinder<K, E>,
    private val keyMapper: KeyMapper<K, E>,
    private val watchEngine: WatchEngine
) {

    fun key(entity: E): K {
        return keyMapper.fromInstance(entity)
    }

    fun create(entity: E) {
        val key = keyMapper.fromInstance(entity)
        val relativeRecord = pathfinder.findRecord(key)
        val destination = root.resolve(relativeRecord)
        if (Files.exists(destination)) {
            throw EntityIoException.AlreadyExists("Entity already exists with key: $key")
        }
        createParentIfNotExists(key, destination)
        val tempFile = destination.resolveSibling(destination.fileName.toString() + ".tmp")
        try {
            Files.newOutputStream(tempFile, StandardOpenOption.CREATE_NEW).use { outputStream ->
                writer.writeValue(outputStream, entity)
            }
            Files.move(tempFile, destination, StandardCopyOption.ATOMIC_MOVE)
        } catch (t: Throwable) {
            throw EntityIoException.WriteFailure("Failed to write entity", t)
        }
    }

    fun deleteOnExit(entity: E) {
        val key = keyMapper.fromInstance(entity)
        val relativeRecord = pathfinder.findRecord(key)
        val destination = root.resolve(relativeRecord)
        if (destination.notExists()) {
            throw EntityIoException.NotFound(key)
        }
        try {
            destination.toFile().deleteOnExit()
        } catch (t: Throwable) {
            throw EntityIoException.DeleteOnExitFailure(key)
        }
    }

    fun read(key: K): E {
        val entityPath = pathfinder.findRecord(key)
        val file = root.resolve(entityPath)
        return read(file)
    }

    fun reread(entity: E): E {
        return read(keyMapper.fromInstance(entity))
    }

    private fun createParentIfNotExists(key: K, destination: Path) {
        val absoluteParent = destination.parent
        if (!Files.exists(absoluteParent)) {
            try {
                Files.createDirectories(absoluteParent)
            } catch (t: Throwable) {
                val message = "Failed to create parent folder for $key"
                throw EntityIoException.WriteFailure(message, t)
            }
        }
    }

    fun update(entity: E) {
        val key = keyMapper.fromInstance(entity)
        val relativeRecord = pathfinder.findRecord(key)
        val destination = root.resolve(relativeRecord)
        if (Files.notExists(destination)) {
            throw EntityIoException.NotFound(key)
        }
        createParentIfNotExists(key, destination)
        val tempFile = destination.resolveSibling(destination.fileName.toString() + ".tmp")
        Files.newOutputStream(tempFile, StandardOpenOption.CREATE_NEW).use { outputStream ->
            writer.writeValue(outputStream, entity)
        }
        Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
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

    fun delete(key: K) {
        val file = root.resolve(pathfinder.findRecord(key))
        if (Files.notExists(file)) {
            throw EntityIoException.NotFound(key)
        }
        Files.delete(file)
    }

    fun delete(entity: E) {
        delete(keyMapper.fromInstance(entity))
    }

    fun stream(keyFilter: Predicate<K>? = null): Stream<E> {
        val allPathsMappedToKeys = pathfinder.streamAll()
                .map { recordPath: Path -> keyMapper.fromRelativeRecord(recordPath) }
        val allKeysForRead = keyFilter?.let { allPathsMappedToKeys.filter(it) } ?: allPathsMappedToKeys
        return allKeysForRead.map { read(root.resolve(pathfinder.findRecord(it))) }
    }

    fun watch(keyFilter: Predicate<K>? = null): Observable<EntityEvent<K, E>> {
        return root.watch(recursive = true)
                .filter { pathfinder.isRecord(root.relativize(it.file)) }
                .map { event: PathWatchEvent ->
                    PreReadWatchEventPayload(
                            event = event,
                            key = keyMapper.fromRelativeRecord(root.relativize(event.file))
                    )
                }
                .filter { keyFilter?.test(it.key) != false }
                .map {
                    val entity = if (it.event.kind != StandardWatchEventKinds.ENTRY_DELETE
                            && Files.exists(it.event.file)
                            && Files.size(it.event.file) > 0
                    ) {
                        try {
                            read(it.event.file)
                        } catch (t: Throwable) {
                            null
                        }
                    } else {
                        null
                    }
                    PostReadWatchEventPayload(it.event, it.key, entity)
                }
                .filter {
                    when (it.event.kind) {
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE -> it.entity != null
                        StandardWatchEventKinds.ENTRY_DELETE -> it.entity == null
                        StandardWatchEventKinds.OVERFLOW -> true
                        else -> false
                    }
                }
                .map {
                    val state = when(it.event.kind) {
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
        val event: PathWatchEvent,
        val key: K
    )

    private class PostReadWatchEventPayload<K : Key, E : Entity<K>>(
            val event: PathWatchEvent,
            val key: K,
            val entity: E?
    )
}