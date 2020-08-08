package org.coner.snoozle.db.sample

import org.coner.snoozle.db.entity.VersionedEntity
import org.coner.snoozle.db.entity.VersionedEntityContainer
import java.time.ZonedDateTime
import java.util.*

data class Gadget(
        val id: UUID = UUID.randomUUID(),
        var name: String? = null,
        var silly: ZonedDateTime? = null
) : VersionedEntity {
    class VersionContainer(
            entity: Gadget,
            version: Int,
            ts: ZonedDateTime
    ) : VersionedEntityContainer<Gadget>(entity, version, ts)
}