package tech.coner.snoozle.db

import tech.coner.snoozle.db.path.Pathfinder
import tech.coner.snoozle.db.path.RelativePath
import java.util.*

class KeyMapper<K : tech.coner.snoozle.db.Key, R : tech.coner.snoozle.db.Record<K>>(
    private val definition: tech.coner.snoozle.db.RecordDefinition<K, R>,
    private val pathfinder: Pathfinder<K, R>,
    private val relativeRecordFn: tech.coner.snoozle.db.KeyMapper<K, R>.RelativeRecordContext.() -> K,
    private val instanceFn: R.() -> K
) {

    fun fromRelativeRecord(relativeRecord: RelativePath): K {
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
            return UUID.fromString(rawStringParts[index])
        }

        fun stringAt(index: Int): String {
            return rawStringParts[index]
        }
    }

}
