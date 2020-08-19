package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.path.PathPart
import java.util.*

class EntityKeyParser<E : Entity<K>, K : Key>(
        private val uuidPathPartExtractors: Array<PathPart.UuidVariable<E, K>?>,
        private val stringPathPartExtractors: Array<PathPart.StringVariable<E, K>?>,
        private val fn: EntityKeyParser<E, K>.Context.() -> K
) {

    fun parse(rawStringParts: Array<String>): K {
        return fn.invoke(Context(rawStringParts))
    }

    inner class Context(
            private val rawStringParts: Array<String>
    ) {
        fun uuidAt(index: Int): UUID {
            return uuidPathPartExtractors[index]?.keyValueFromPathPart(rawStringParts[index])
                    ?: throw IllegalArgumentException("No UuidVariable at index $index")
        }

        fun stringAt(index: Int): String {
            return stringPathPartExtractors[index]?.keyValueFromPathPart(rawStringParts[index])
                    ?: throw IllegalArgumentException("No StringVariable at index $index")
        }
    }

}
