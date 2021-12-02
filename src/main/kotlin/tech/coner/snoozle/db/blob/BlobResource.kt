package tech.coner.snoozle.db.blob

import tech.coner.snoozle.db.KeyMapper
import tech.coner.snoozle.db.Pathfinder
import tech.coner.snoozle.db.Record
import tech.coner.snoozle.util.readText
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

class BlobResource<B : Blob> constructor(
    private val root: Path,
    private val definition: BlobDefinition<B>,
    private val pathfinder: Pathfinder<B, Record<B>>,
    private val keyMapper: KeyMapper<B, Record<B>>
) {
    fun getAsText(blob: B): String {
        val file = getAbsolutePathTo(blob)
        return if (Files.exists(file)) {
            file.readText()
        } else {
            throw BlobIoException(BlobIoException.Reason.NotFound)
        }
    }

    fun getAsInputStream(blob: B): InputStream {
        val file = getAbsolutePathTo(blob)
        return if (Files.exists(file)) {
            file.toFile().inputStream().buffered()
        } else {
            throw BlobIoException(BlobIoException.Reason.NotFound)
        }
    }

    fun getAbsolutePathTo(blob: B): Path {
        val blobPath = pathfinder.findRecord(blob)
        return root.resolve(blobPath)
    }

    fun put(blob: B, text: String) {
        val blobPath = pathfinder.findRecord(blob)
        val file = root.resolve(blobPath)
        Files.createDirectories(file.parent)
        file.toFile().writeText(text)
    }

    fun put(blob: B, bytes: ByteArray) {
        val blobPath = pathfinder.findRecord(blob)
        val file = root.resolve(blobPath)
        Files.createDirectories(file.parent)
        file.toFile().outputStream().buffered().use {
            it.write(bytes)
        }
    }

    fun stream(): Stream<B> {
        return pathfinder.streamAll()
                .map { recordPath: Path -> keyMapper.fromRelativeRecord(recordPath) }
    }

}