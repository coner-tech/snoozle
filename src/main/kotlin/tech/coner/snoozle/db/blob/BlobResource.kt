package tech.coner.snoozle.db.blob

import tech.coner.snoozle.db.KeyMapper
import tech.coner.snoozle.db.Pathfinder
import tech.coner.snoozle.db.Record
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.readText
import tech.coner.snoozle.db.AbsolutePath
import tech.coner.snoozle.db.RelativePath
import tech.coner.snoozle.db.asAbsolute

class BlobResource<B : Blob>(
    private val root: AbsolutePath,
    private val definition: BlobDefinition<B>,
    private val pathfinder: Pathfinder<B, Record<B>>,
    private val keyMapper: KeyMapper<B, Record<B>>
) {
    fun getAsText(blob: B): String {
        val file = getAbsolutePathTo(blob).value
        return if (Files.exists(file)) {
            try {
                file.readText()
            } catch (t: Throwable) {
                throw BlobIoException(BlobIoException.Reason.ReadFailure, t)
            }
        } else {
            throw BlobIoException(BlobIoException.Reason.NotFound)
        }
    }

    fun getAsInputStream(blob: B): InputStream {
        val file = getAbsolutePathTo(blob).value
        return if (Files.exists(file)) {
            try {
                file.toFile().inputStream().buffered()
            } catch (t: Throwable) {
                throw BlobIoException(BlobIoException.Reason.ReadFailure, t)
            }
        } else {
            throw BlobIoException(BlobIoException.Reason.NotFound)
        }
    }

    fun getAbsolutePathTo(blob: B): AbsolutePath {
        val blobPath = pathfinder.findRecord(blob)
        return root.value.resolve(blobPath.value).asAbsolute()
    }

    fun put(blob: B, text: String) {
        val blobPath = pathfinder.findRecord(blob)
        val file = root.value.resolve(blobPath.value)
        Files.createDirectories(file.parent)
        file.toFile().writeText(text)
    }

    fun put(blob: B, bytes: ByteArray) {
        val blobPath = pathfinder.findRecord(blob)
        val file = root.value.resolve(blobPath.value)
        Files.createDirectories(file.parent)
        file.toFile().outputStream().buffered().use {
            it.write(bytes)
        }
    }

    fun stream(): Stream<B> {
        return pathfinder.streamAll()
                .map { recordPath: RelativePath -> keyMapper.fromRelativeRecord(recordPath) }
    }

}