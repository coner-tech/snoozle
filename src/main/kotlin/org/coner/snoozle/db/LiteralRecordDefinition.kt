package org.coner.snoozle.db

import org.coner.snoozle.db.path.PathPart
import java.util.*

abstract class LiteralRecordDefinition<R : Record> : RecordDefinition<R>() {

    operator fun String.div(uuidExtractor: R.() -> UUID): MutableList<PathPart<R>> {
        return mutableListOf(
                PathPart.StringValue(this),
                PathPart.DirectorySeparator(),
                PathPart.UuidVariable(uuidExtractor)
        )
    }

    operator fun MutableList<PathPart<R>>.div(part: String): MutableList<PathPart<R>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.StringValue(part))
        return this
    }

    operator fun MutableList<PathPart<R>>.div(uuidExtractor: R.() -> UUID): MutableList<PathPart<R>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.UuidVariable(uuidExtractor))
        return this
    }

    operator fun MutableList<PathPart<R>>.plus(extension: String): MutableList<PathPart<R>> {
        add(PathPart.StringValue(extension))
        return this
    }

    operator fun MutableList<PathPart<R>>.div(stringExtractor: StringArgumentExtractor<R>): MutableList<PathPart<R>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.StringVariable(stringExtractor::extract))
        return this
    }

    operator fun MutableList<PathPart<R>>.plus(stringExtractor: StringArgumentExtractor<R>): MutableList<PathPart<R>> {
        add(PathPart.StringVariable(stringExtractor::extract))
        return this
    }

    operator fun MutableList<PathPart<R>>.div(versionArgument: PathPart.VersionArgumentVariable<R>): MutableList<PathPart<R>> {
        add(PathPart.DirectorySeparator())
        add(versionArgument)
        return this
    }
}