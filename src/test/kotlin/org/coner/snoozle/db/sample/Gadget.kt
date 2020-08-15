package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.entity.VersionedEntity
import java.time.ZonedDateTime
import java.util.*

data class Gadget(
        val id: UUID = UUID.randomUUID(),
        var name: String? = null,
        var silly: ZonedDateTime? = null
) : VersionedEntity<GadgetKey> {

    override val key by lazy { GadgetKey(id = id) }
}

data class GadgetKey(val id: UUID) : Key
