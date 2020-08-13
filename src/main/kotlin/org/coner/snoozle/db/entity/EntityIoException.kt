package org.coner.snoozle.db.entity

sealed class EntityIoException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotFound(message: String, cause: Throwable? = null) : EntityIoException(message, cause) {
        constructor(args: List<Any>) : this("Not found for args: ${args.joinToString(", ")}")
    }
    class WriteFailure(message: String, cause: Throwable? = null) : EntityIoException(message, cause)
    class ReadFailure(message: String, cause: Throwable? = null) : EntityIoException(message, cause)
}