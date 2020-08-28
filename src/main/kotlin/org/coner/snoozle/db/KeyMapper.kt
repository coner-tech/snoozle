package org.coner.snoozle.db

import java.nio.file.Path
import java.util.*

class KeyMapper<K : Key, R : Record<K>>(
        private val definition: RecordDefinition<K, R>,
        private val pathfinder: Pathfinder<K, R>,
        private val relativeRecordFn: KeyMapper<K, R>.RelativeRecordContext.() -> K,
        private val instanceFn: R.() -> K
) {

    private val uuidPathPartExtractors: Array<PathPart.UuidVariable<K, R>?> by lazy {
        definition.path
                .map { when (it) {
                    is PathPart.UuidVariable -> it
                    else -> null
                } }
                .toTypedArray()
    }
    private val stringPathPartExtractors: Array<PathPart.StringVariable<K, R>?> by lazy {
        definition.path
                .map { when (it) {
                    is PathPart.StringVariable -> it
                    else -> null
                } }
                .toTypedArray()
    }

    fun fromRelativeRecord(relativeRecord: Path): K {
        val rawStringParts = pathfinder.findVariableStringParts(relativeRecord)
        return relativeRecordFn.invoke(RelativeRecordContext(rawStringParts))
    }

    fun fromInstance(instance: R): K {
        return instanceFn(instance)
    }

    inner class RelativeRecordContext(
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
