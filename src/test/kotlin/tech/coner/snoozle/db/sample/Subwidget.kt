package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.entity.Entity
import java.util.*

data class Subwidget(
        val id: UUID = UUID.randomUUID(),
        val widgetId: UUID,
        val name: String
) : Entity<Subwidget.Key> {

    data class Key(
            val id: UUID,
            val widgetId: UUID
    ) : tech.coner.snoozle.db.Key

}
