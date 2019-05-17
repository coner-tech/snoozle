package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.EntityPath
import org.coner.snoozle.db.VersionedEntity
import java.util.*

@EntityPath("/gadgets/{id}")
@VersionedEntity
data class Gadget(
        val id: UUID = UUID.randomUUID(),
        var name: String? = null
) : Entity