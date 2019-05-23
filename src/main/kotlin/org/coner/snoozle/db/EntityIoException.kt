package org.coner.snoozle.db

sealed class EntityIoException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotFound(message: String, cause: Throwable? = null) : EntityIoException(message, cause)
    class WriteFailure(message: String, cause: Throwable? = null) : EntityIoException(message, cause)
    class ReadFailure(message: String, cause: Throwable? = null) : EntityIoException(message, cause)
}