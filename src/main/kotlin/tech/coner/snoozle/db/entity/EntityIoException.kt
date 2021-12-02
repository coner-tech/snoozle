package tech.coner.snoozle.db.entity

import tech.coner.snoozle.db.Key

sealed class EntityIoException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class CreateFailure(message: String, cause: Throwable? = null) : EntityIoException(message, cause)
    class NotFound(message: String, cause: Throwable? = null) : EntityIoException(message, cause) {
        constructor(key: tech.coner.snoozle.db.Key) : this("Not found for key: $key")
    }
    class WriteFailure(message: String, cause: Throwable? = null) : EntityIoException(message, cause)
    class ReadFailure(message: String, cause: Throwable? = null) : EntityIoException(message, cause)
}