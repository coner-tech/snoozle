package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.entity.Entity
import java.time.ZonedDateTime
import java.util.*

data class Gadget(
        val id: UUID = UUID.randomUUID(),
        var name: String? = null,
        var silly: ZonedDateTime? = null
) : Entity<Gadget.Key> {

    data class Key(val id: UUID) : org.coner.snoozle.db.Key

}
