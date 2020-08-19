package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.Record
import java.time.ZonedDateTime
import java.util.*

data class VersionedEntityContainer<E : VersionedEntity<EK>, EK : Key>(
        val entity: E,
        val version: Int,
        val ts: ZonedDateTime
) : Entity<VersionedEntityContainerKey<EK>>, Comparable<VersionedEntityContainer<E, EK>> {

        override fun compareTo(other: VersionedEntityContainer<E, EK>): Int {
                return version.compareTo(other.version)
        }

        override val key by lazy { VersionedEntityContainerKey(entity = entity, version = version )}
}

data class VersionedEntityContainerKey<EK : Key>(
        val entity: Entity<EK>,
        val version: Int
) : Key