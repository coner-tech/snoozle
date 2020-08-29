package org.coner.snoozle.db.sample

import org.coner.snoozle.db.entity.Entity
import org.coner.snoozle.db.Key
import java.util.*

data class Widget(
        val id: UUID = UUID.randomUUID(),
        val name: String
) : Entity<Widget.Key> {

    data class Key(val id: UUID) : org.coner.snoozle.db.Key
}

