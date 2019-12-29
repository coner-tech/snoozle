package org.coner.snoozle.db.blob

class BlobIoException(reason: Reason, cause: Throwable? = null) : Exception(reason.message, cause) {
    enum class Reason(val message: String) {
        NotFound("Blob not found"),
        WriteFailure("Failed to write blob"),
        ReadFailure("Failed to read blob")
    }
}