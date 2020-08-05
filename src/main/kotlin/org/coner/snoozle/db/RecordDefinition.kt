package org.coner.snoozle.db

import org.coner.snoozle.db.path.PathPart
import java.util.*

abstract class RecordDefinition<R : Record> {
    var path: List<PathPart<R>> = mutableListOf()

    operator fun String.div(uuidExtractor: R.() -> UUID): MutableList<PathPart<R>> {
        return mutableListOf(
                PathPart.StringPathPart(this),
                PathPart.DirectorySeparatorPathPart(),
                PathPart.UuidVariablePathPart(uuidExtractor)
        )
    }

    operator fun MutableList<PathPart<R>>.div(part: String): MutableList<PathPart<R>> {
        add(PathPart.DirectorySeparatorPathPart())
        add(PathPart.StringPathPart(part))
        return this
    }

    operator fun MutableList<PathPart<R>>.div(uuidExtractor: R.() -> UUID): MutableList<PathPart<R>> {
        add(PathPart.DirectorySeparatorPathPart())
        add(PathPart.UuidVariablePathPart(uuidExtractor))
        return this
    }

    operator fun MutableList<PathPart<R>>.plus(extension: String): MutableList<PathPart<R>> {
        add(PathPart.StringPathPart(extension))
        return this
    }

    operator fun MutableList<PathPart<R>>.div(stringExtractor: StringExtractor<R>): MutableList<PathPart<R>> {
        add(PathPart.DirectorySeparatorPathPart())
        add(PathPart.StringVariablePathPart(stringExtractor::extract))
        return this
    }

    operator fun MutableList<PathPart<R>>.plus(stringExtractor: StringExtractor<R>): MutableList<PathPart<R>> {
        add(PathPart.StringVariablePathPart(stringExtractor::extract))
        return this
    }

    class StringExtractor<R : Record>(private val fn: R.() -> String) {
        fun extract(record: R): String {
            return fn(record)
        }
    }

    fun string(stringExtractor: R.() -> String): StringExtractor<R> {
        return StringExtractor(stringExtractor)
    }

}