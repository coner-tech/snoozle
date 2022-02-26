package tech.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import tech.coner.snoozle.db.migration.MigrationDefinition
import tech.coner.snoozle.db.migration.Segment

class MigrationsRegistry(
    val migrations: Map<Segment, MigrationDefinition>
) {

    class Builder(
        private val objectMapper: ObjectMapper,
    ) {
        private val migrations: MutableMap<Segment, MigrationDefinition> = mutableMapOf()

        fun migrate(segment: Segment, op: MigrationDefinition.Builder.() -> Unit) {
            migrations[segment] = MigrationDefinition.Builder(
                objectMapper = objectMapper,
                segment = segment
            )
                .apply(op)
                .build()
        }

        fun migrate(segment: Pair<Int?, Int>, op: MigrationDefinition.Builder.() -> Unit) {
            migrate(Segment(segment.first, segment.second), op)
        }

        fun build(): MigrationsRegistry {
            return MigrationsRegistry(
                migrations = migrations
            )
        }
    }
}
