package org.coner.snoozle.db.versioning

enum class EntityVersioningStrategy {
    @Deprecated(message = "Migrate to Discrete")
    AutomaticInternalVersioning,
    Discrete
}