package tech.coner.snoozle.db.migration

import com.fasterxml.jackson.databind.ObjectMapper

class MigrationDefinition(
    val segment: Segment,
    val tasks: List<MigrationTask>
) {

    class Builder(
        private val objectMapper: ObjectMapper,
        private val segment: Segment
    ) {
        private val tasks = mutableListOf<MigrationTask>()

        operator fun String.div(matcher: MigrationPathMatcher): MutableList<MigrationPathMatcher> {
            return mutableListOf(
                MigrationPathMatcher.OnString(this),
                MigrationPathMatcher.OnDirectorySeparator,
                matcher
            )
        }

        operator fun MutableList<MigrationPathMatcher>.div(matcher: MigrationPathMatcher): MutableList<MigrationPathMatcher> {
            add(MigrationPathMatcher.OnDirectorySeparator)
            add(matcher)
            return this
        }

        operator fun MutableList<MigrationPathMatcher>.div(value: String): MutableList<MigrationPathMatcher> {
            add(MigrationPathMatcher.OnDirectorySeparator)
            add(MigrationPathMatcher.OnString(value))
            return this
        }

        operator fun MutableList<MigrationPathMatcher>.plus(value: String): MutableList<MigrationPathMatcher> {
            add(MigrationPathMatcher.OnString(value))
            return this
        }

        fun matchUuid(): MigrationPathMatcher = MigrationPathMatcher.OnUuid

        fun move(
            from: List<MigrationPathMatcher>,
            to: List<MigrationPathMatcher>
        ) {
            tasks += MigrationTask.Move(from = from, to = to)
        }

        fun deleteDirectories(
            matching: List<MigrationPathMatcher>
        ) {
            tasks += MigrationTask.DeleteDirectories(matching)
        }

        fun deleteDirectory(
            name: String
        ) {
            tasks += MigrationTask.DeleteDirectories(
                matching = listOf(MigrationPathMatcher.OnString(name))
            )
        }

        fun onEntities(
            on: List<MigrationPathMatcher>,
            op: MigrationTask.MutateEntities.Builder.() -> Unit
        ) {
            tasks += MigrationTask.MutateEntities.Builder(
                objectMapper = objectMapper,
                on = on
            )
                .apply(op)
                .build()
        }

        fun build() : MigrationDefinition {
            return MigrationDefinition(
                segment = segment,
                tasks = tasks
            )
        }
    }
}