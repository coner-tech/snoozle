package tech.coner.snoozle.db

import tech.coner.snoozle.db.path.PathPart
import kotlin.reflect.KClass

abstract class RecordDefinition<K : Key, R : Record<K>>(
        val keyClass: KClass<K>,
        val recordClass: KClass<R>
) {

    var path: List<PathPart<K, R, *>> = mutableListOf()
    var keyFromPath: (KeyMapper<K, R>.RelativeRecordContext.() -> K)? = null

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

val <K : Key, R : Record<K>> List<PathPart<K, R, *>>.parent: List<PathPart<K, R, *>>
    get() = when (val indexOfLastDirectorySeparator = indexOfLast { it is PathPart.DirectorySeparator<*, *> }) {
            in Int.MIN_VALUE..(-1) -> emptyList()
            else -> take(indexOfLastDirectorySeparator)
        }