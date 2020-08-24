package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.PathPart
import org.coner.snoozle.db.Pathfinder
import java.nio.file.Path
import java.util.*

class VersionedEntityKeyParser<E : Entity<K>, K : Key>(
        private val definition: EntityDefinition<E, K>,
        private val path: Pathfinder<E, K>,
        private val fn: VersionedEntityKeyParser<E, K>.Context.() -> K
) {

    private val uuidPathPartExtractors: Array<PathPart.UuidVariable<E, K>?> by lazy {
        definition.path
                .map { when (it) {
                    is PathPart.UuidVariable -> it
                    else -> null
                } }
                .toTypedArray()
    }
    private val stringPathPartExtractors: Array<PathPart.StringVariable<E, K>?> by lazy {
        definition.path
                .map { when (it) {
                    is PathPart.StringVariable -> it
                    else -> null
                } }
                .toTypedArray()
    }

    fun parse(relativeRecord: Path): K {
        val rawStringParts = path.findVariableStringParts(relativeRecord)
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
