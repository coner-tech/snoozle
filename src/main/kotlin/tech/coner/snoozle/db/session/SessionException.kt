package tech.coner.snoozle.db.session

sealed class SessionException(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    class AlreadyExists : SessionException("session already exists")
    class AlreadyClosed : SessionException("Session already closed")
    class NotReadyToMigrate(message: String, cause: Throwable? = null) : SessionException(message, cause)
}