package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Record
import java.time.ZonedDateTime
import java.util.*

abstract class VersionedEntityContainer<E : VersionedEntity>(
        val entity: E,
        val version: Int,
        val ts: ZonedDateTime
) : Record {

        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is VersionedEntityContainer<*>) return false

                if (entity != other.entity) return false
                if (version != other.version) return false
                if (ts != other.ts) return false

                return true
        }

        override fun hashCode(): Int {
                var result = entity.hashCode()
                result = 31 * result + version
                result = 31 * result + ts.hashCode()
                return result
        }
}