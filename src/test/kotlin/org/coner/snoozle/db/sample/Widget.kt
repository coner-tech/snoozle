package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.EntityPath
import java.util.*

data class Widget(
        val id: UUID = UUID.randomUUID(),
        val name: String
) : Entity