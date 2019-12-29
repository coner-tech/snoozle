package org.coner.snoozle.db.sample

import org.coner.snoozle.db.entity.Entity
import java.util.*

data class Widget(
        val id: UUID = UUID.randomUUID(),
        val name: String
) : Entity