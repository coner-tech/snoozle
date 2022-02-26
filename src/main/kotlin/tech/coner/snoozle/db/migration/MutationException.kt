package tech.coner.snoozle.db.migration

class MutationException(
    message: String,
    cause: Throwable? = null
) : Throwable(
    message = message,
    cause = cause
)