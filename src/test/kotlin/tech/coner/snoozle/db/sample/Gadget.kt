package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.entity.Entity
import java.time.ZonedDateTime
import java.util.*

data class Gadget(
        val id: UUID = UUID.randomUUID(),
        var name: String? = null,
        var silly: ZonedDateTime? = null
) : Entity<Gadget.Key> {

    data class Key(val id: UUID) : tech.coner.snoozle.db.Key

    override fun toKey() = Key(id = id)
}
