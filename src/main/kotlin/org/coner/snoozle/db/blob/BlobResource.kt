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

    fun getAsInputStream(vararg args: Any): InputStream {
        val file = getAbsolutePathTo(*args)
        return if (Files.exists(file)) {
            file.toFile().inputStream().buffered()
        } else {
            throw BlobIoException(BlobIoException.Reason.NotFound)
        }
    }

    fun getAbsolutePathTo(vararg args: Any): Path {
        val blobPath = path.findRecordByArgs(*args)
        return root.resolve(blobPath)
    }

    fun getAbsolutePathTo(blob: B): Path {
        val blobPath = path.findRecord(blob)
        return root.resolve(blobPath)
    }

    fun put(blob: B, text: String) {
        val blobPath = path.findRecord(blob)
        val file = root.resolve(blobPath)
        Files.createDirectories(file.parent)
        file.toFile().writeText(text)
    }

    fun put(blob: B, bytes: ByteArray) {
        val blobPath = path.findRecord(blob)
        val file = root.resolve(blobPath)
        Files.createDirectories(file.parent)
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