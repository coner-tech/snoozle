package org.coner.snoozle.db

import java.util.*

abstract class LiteralRecordDefinition<R : Record<K>, K : Key> : RecordDefinition<R, K>() {

    operator fun String.div(uuidExtractor: K.() -> UUID): MutableList<PathPart<R, K, *>> {
        return mutableListOf(
                PathPart.StringValue(this),
                PathPart.DirectorySeparator(),
                PathPart.UuidVariable(uuidExtractor)
        )
    }

    operator fun MutableList<PathPart<R, K, *>>.div(part: String): MutableList<PathPart<R, K, *>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.StringValue(part))
        return this
    }

    operator fun MutableList<PathPart<R, K, *>>.div(uuidExtractor: K.() -> UUID): MutableList<PathPart<R, K, *>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.UuidVariable(uuidExtractor))
        return this
    }

    operator fun MutableList<PathPart<R, K, *>>.plus(extension: String): MutableList<PathPart<R, K, *>> {
        add(PathPart.StringValue(extension))
        return this
    }

    operator fun MutableList<PathPart<R, K, *>>.div(stringExtractor: StringArgumentExtractor<K>): MutableList<PathPart<R, K, *>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.StringVariable(stringExtractor::extract))
        return this
    }

    operator fun MutableList<PathPart<R, K, *>>.plus(stringExtractor: StringArgumentExtractor<K>): MutableList<PathPart<R, K, *>> {
        add(PathPart.StringVariable(stringExtractor::extract))
        return this
    }
}