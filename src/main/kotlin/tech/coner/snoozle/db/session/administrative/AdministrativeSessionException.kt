package tech.coner.snoozle.db.session.administrative

import tech.coner.snoozle.db.metadata.SessionMetadataEntity

sealed class AdministrativeSessionException(
    message: String,
    cause: Throwable? = null
) : Throwable(message, cause) {
    class ConcurrentSessionsNotPermitted(val sessions: List<SessionMetadataEntity>) : AdministrativeSessionException("""
        Cannot open administrative session while concurrent sessions exist: $sessions
    """.trimIndent())
    class MetadataWriteFailure(cause: Throwable) : Throwable("Failed to write admin session metadata", cause)
    class Unknown(cause: Throwable) : AdministrativeSessionException("Something went wrong opening an administrative session", cause)
}