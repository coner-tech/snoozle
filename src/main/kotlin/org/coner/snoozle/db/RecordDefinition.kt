package org.coner.snoozle.db

import org.coner.snoozle.db.path.PathPart
import java.util.*

abstract class RecordDefinition<R : Record<K>, K : Key> {
    var path: List<PathPart<R, K, *>> = mutableListOf()

    abstract class PathArgumentExtractor<K : Key, P>(private val fn: K.() -> P) {
        fun extract(key: K): P {
            return fn(key)
        }
    }

    class StringArgumentExtractor<K : Key>(fn: K.() -> String) : PathArgumentExtractor<K, String>(fn)

    fun string(extractor: K.() -> String): StringArgumentExtractor<K> {
        return StringArgumentExtractor(extractor)
    }

}