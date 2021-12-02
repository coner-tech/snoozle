package tech.coner.snoozle.db

import java.util.*
import kotlin.reflect.KClass

abstract class LiteralRecordDefinition<K : tech.coner.snoozle.db.Key, R : Record<K>>(
        keyClass: KClass<K>,
        recordClass: KClass<R>
) : RecordDefinition<K, R>(keyClass, recordClass) {

    operator fun String.div(uuidExtractor: K.() -> UUID): MutableList<PathPart<K, R, *>> {
        return mutableListOf(
                PathPart.StringValue(this),
                PathPart.DirectorySeparator(),
                PathPart.UuidVariable(uuidExtractor)
        )
    }

    operator fun MutableList<PathPart<K, R, *>>.div(part: String): MutableList<PathPart<K, R, *>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.StringValue(part))
        return this
    }

    operator fun MutableList<PathPart<K, R, *>>.div(uuidExtractor: K.() -> UUID): MutableList<PathPart<K, R, *>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.UuidVariable(uuidExtractor))
        return this
    }

    operator fun MutableList<PathPart<K, R, *>>.plus(extension: String): MutableList<PathPart<K, R, *>> {
        add(PathPart.StringValue(extension))
        return this
    }

    operator fun MutableList<PathPart<K, R, *>>.div(stringExtractor: StringArgumentExtractor<K>): MutableList<PathPart<K, R, *>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.StringVariable(stringExtractor::extract))
        return this
    }

    operator fun MutableList<PathPart<K, R, *>>.plus(stringExtractor: StringArgumentExtractor<K>): MutableList<PathPart<K, R, *>> {
        add(PathPart.StringVariable(stringExtractor::extract))
        return this
    }
}