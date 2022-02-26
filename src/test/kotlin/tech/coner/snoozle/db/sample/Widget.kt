package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.entity.Entity
import java.util.*

data class Widget(
        val id: UUID = UUID.randomUUID(),
        val name: String,
        val widget: Boolean
) : Entity<Widget.Key> {

    data class Key(val id: UUID) : tech.coner.snoozle.db.Key
}

