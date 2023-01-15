package tech.coner.snoozle.db.metadata

import tech.coner.snoozle.db.blob.BlobIoException
import tech.coner.snoozle.db.entity.EntityIoException
import tech.coner.snoozle.util.resolve
import java.nio.file.Files
import java.util.*
import tech.coner.snoozle.db.path.AbsolutePath

class MetadataRepository(
    private val root: AbsolutePath,
    private val databaseVersionResource: DatabaseVersionResource,
    private val sessionMetadataResource: SessionMetadataResource
) {

    fun rootContainsAnythingOtherThanCurrentSessionMetadata(id: UUID): Boolean {
        val expectedCurrentSessionMetadata = root.value.resolve(".snoozle", "sessions", "$id.json")
        return Files
            .find(root.value, 3, { _, attrs -> attrs.isRegularFile })
            .anyMatch { it != expectedCurrentSessionMetadata }
    }

    fun readVersion(): Int? {
        return try {
            databaseVersionResource.getAsText(DatabaseVersionBlob).toInt()
        } catch (blobIoException: BlobIoException) {
            when (blobIoException.reason) {
                BlobIoException.Reason.NotFound -> null
                else -> throw MetadataException("Failed to read version blob", blobIoException)
            }
        } catch (t: Throwable) {
            throw MetadataException("Failed to read version blob", t)
        }
    }

    fun writeVersion(version: Int) {
        databaseVersionResource.put(DatabaseVersionBlob, version.toString())
    }

    fun writeNewSessionMetadata(sessionMetadata: SessionMetadataEntity) {
        try {
            sessionMetadataResource.create(sessionMetadata)
        } finally {
            try {
                sessionMetadataResource.deleteOnExit(sessionMetadata)
            } catch (notFound: EntityIoException.NotFound) {
                // swallow. no need to delete what cannot be found
            }
        }
    }

    fun deleteSessionMetadata(sessionMetadataKey: SessionMetadataEntity.Key) {
        sessionMetadataResource.delete(sessionMetadataKey)
    }

    fun listSessions(): List<SessionMetadataEntity> {
        return sessionMetadataResource.stream().toList()
    }
}
