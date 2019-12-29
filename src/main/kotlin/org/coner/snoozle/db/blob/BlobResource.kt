package org.coner.snoozle.db.blob

import org.coner.snoozle.db.path.Pathfinder
import org.coner.snoozle.util.readText
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

class BlobResource<B : Blob> constructor(
        private val root: Path,
        private val definition: BlobDefinition<B>,
        private val path: Pathfinder<B>
) {
    fun getAsText(blob: B): String {
        val blobPath = path.findRecord(blob)
        val file = root.resolve(blobPath)
        return if (Files.exists(file)) {
            file.readText()
        } else {
            throw BlobIoException(BlobIoException.Reason.NotFound)
        }
    }

    fun getAsInputStream(blob: B): InputStream {
        val blobPath = path.findRecord(blob)
        val file = root.resolve(blobPath)
        return if (Files.exists(file)) {
            file.toFile().inputStream().buffered()
        } else {
            throw BlobIoException(BlobIoException.Reason.NotFound)
        }
    }

    fun getAsInputStream(vararg args: Any): InputStream {
        val blobPath = path.findRecordByArgs(*args)
        val file = root.resolve(blobPath)
        return if (Files.exists(file)) {
            file.toFile().inputStream().buffered()
        } else {
            throw BlobIoException(BlobIoException.Reason.NotFound)
        }
    }

    fun put(blob: B, text: String) {
        val blobPath = path.findRecord(blob)
        val file = root.resolve(blobPath)
        file.toFile().writeText(text)
    }

    fun put(blob: B, bytes: ByteArray) {
        val blobPath = path.findRecord(blob)
        val file = root.resolve(blobPath)
        file.toFile().outputStream().buffered().use {
            it.write(bytes)
        }
    }

    fun list(vararg args: Any): List<B> {
        val listingPath = path.findListingByArgs(*args)
        val listing = root.resolve(listingPath)
        return Files.list(listing)
                .filter { Files.isRegularFile(it) && path.isRecord(root.relativize(it)) }
                .sorted(compareBy(Path::toString))
                .map { definition.factory.factory(root.relativize(it)) }
                .toList()
    }

}