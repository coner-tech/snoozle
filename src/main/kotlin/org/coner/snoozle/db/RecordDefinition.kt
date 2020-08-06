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

    operator fun MutableList<PathPart<R>>.div(stringExtractor: StringArgumentExtractor<R>): MutableList<PathPart<R>> {
        add(PathPart.DirectorySeparatorPathPart())
        add(PathPart.StringVariablePathPart(stringExtractor::extract))
        return this
    }

    operator fun MutableList<PathPart<R>>.plus(stringExtractor: StringArgumentExtractor<R>): MutableList<PathPart<R>> {
        add(PathPart.StringVariablePathPart(stringExtractor::extract))
        return this
    }

    operator fun MutableList<PathPart<R>>.div(discreteVersionArgument: PathPart.DiscreteVersionArgumentPathPart<R>): MutableList<PathPart<R>> {
        add(PathPart.DirectorySeparatorPathPart())
        add(discreteVersionArgument)
        return this
    }

    abstract class PathArgumentExtractor<R : Record, P>(private val fn: R.() -> P) {
        fun extract(record: R): P {
            return fn(record)
        }
    }

    class StringArgumentExtractor<R : Record>(fn: R.() -> String) : PathArgumentExtractor<R, String>(fn)
    class IntArgumentExtractor<R : Record>(fn: R.() -> Int) : PathArgumentExtractor<R, Int>(fn)

    fun string(extractor: R.() -> String): StringArgumentExtractor<R> {
        return StringArgumentExtractor(extractor)
    }
    fun int(extractor: R.() -> Int) = IntArgumentExtractor(extractor)
    fun discreteVersion() = PathPart.DiscreteVersionArgumentPathPart<R>()

}