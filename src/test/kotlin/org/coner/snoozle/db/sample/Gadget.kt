package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.EntityPath
import org.coner.snoozle.db.AutomaticVersionedEntity
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

data class Gadget(
        val id: UUID = UUID.randomUUID(),
        var name: String? = null,
        var silly: ZonedDateTime? = null
) : Entity