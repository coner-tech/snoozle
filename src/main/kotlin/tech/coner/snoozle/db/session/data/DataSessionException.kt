package tech.coner.snoozle.db.session.data

sealed class DataSessionException(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    class VersionMismatch(val requiredVersion: Int, val actualVersion: Int?) : Throwable("Database version on disk ($actualVersion) doesn't match defined version ($requiredVersion)")
    class VersionUndefined : Throwable("Database version not defined")
    class VersionReadFailure : Throwable("Failed to read version")
    class MetadataWriteFailure(cause: Throwable) : Throwable("Failed to write data session metadata", cause)
    class Unknown(cause: Throwable) : Throwable("Something went wrong opening a data session", cause)
}