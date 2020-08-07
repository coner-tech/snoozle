package org.coner.snoozle.db

import org.coner.snoozle.db.path.PathPart
import java.util.*

abstract class RecordDefinition<R : Record> {
    var path: List<PathPart<R>> = mutableListOf()

    abstract class PathArgumentExtractor<R : Record, P>(private val fn: R.() -> P) {
        fun extract(record: R): P {
            return fn(record)
        }
    }

    class StringArgumentExtractor<R : Record>(fn: R.() -> String) : PathArgumentExtractor<R, String>(fn)

    fun string(extractor: R.() -> String): StringArgumentExtractor<R> {
        return StringArgumentExtractor(extractor)
    }

}