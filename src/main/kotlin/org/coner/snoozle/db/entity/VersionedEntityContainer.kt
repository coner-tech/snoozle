package org.coner.snoozle.db.entity

import java.time.ZonedDateTime

data class VersionedEntityContainer<E : VersionedEntity>(
        val entity: E,
        val version: VersionArgument,
        val ts: ZonedDateTime
) : Entity