package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Record
import java.time.ZonedDateTime

data class VersionedEntityContainer<E : VersionedEntity>(
        val entity: E,
        val version: Int,
        val ts: ZonedDateTime
) : Record