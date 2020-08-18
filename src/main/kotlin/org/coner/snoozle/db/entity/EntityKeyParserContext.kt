package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.path.PathPart
import java.util.*

class EntityKeyParserContext<E : Entity<K>, K : Key>(
        private val rawStrings: Array<String>,
        private val uuidPathPartExtractors: Array<PathPart.UuidVariable<E, K>?>,
        private val stringPathPartExtractors: Array<PathPart.StringVariable<E, K>?>
) {
    fun uuidAt(index: Int): UUID {
        return uuidPathPartExtractors[index]?.keyValueFromPathPart(rawStrings[index])
                ?: throw IllegalArgumentException("No UuidVariable at index $index")
    }

    fun stringAt(index: Int): String {
        return stringPathPartExtractors[index]?.keyValueFromPathPart(rawStrings[index])
                ?: throw IllegalArgumentException("No StringVariable at index $index")
    }
}