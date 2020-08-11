package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Record
import java.time.ZonedDateTime
import java.util.*

data class VersionedEntityContainer<E : VersionedEntity>(
        val entity: E,
        val version: Int,
        val ts: ZonedDateTime
) : Record, Comparable<VersionedEntityContainer<E>> {

        override fun compareTo(other: VersionedEntityContainer<E>): Int {
                return version.compareTo(other.version)
        }
}