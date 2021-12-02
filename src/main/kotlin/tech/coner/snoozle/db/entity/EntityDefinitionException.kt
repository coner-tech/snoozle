package tech.coner.snoozle.db.entity

/**
 * This exception indicates a snoozle database entity is defined incorrectly.
 *
 * Its message should give some context
 *
 * As long as developers are constructing database instances with a static
 * list of Entity types and performing the barest-minimum testing before
 * shipping, end-users should never experience this.
 */
class EntityDefinitionException(
        message: String,
        cause: Throwable? = null
) : RuntimeException(message, cause)