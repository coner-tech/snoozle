package org.coner.snoozle.db.entity

import java.time.ZonedDateTime

data class DiscreteVersionedEntityContainer<E : DiscreteVersionedEntity>(
        val entity: E,
        val version: DiscreteVersionArgument,
        val ts: ZonedDateTime
) : Entity