package tech.coner.snoozle.db.migration

import tech.coner.snoozle.util.hasUuidPattern
import java.io.File
import java.util.regex.Pattern

sealed class MigrationPathMatcher {

    abstract val regex: Pattern

    class OnString(val value: String) : MigrationPathMatcher() {
        override val regex: Pattern = Pattern.compile(Pattern.quote(value))
    }

    object OnDirectorySeparator : MigrationPathMatcher() {
        override val regex: Pattern = Pattern.compile(Pattern.quote(File.separator))
    }

    object OnUuid : MigrationPathMatcher() {
        override val regex: Pattern = hasUuidPattern
    }
}