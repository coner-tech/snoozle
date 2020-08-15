package org.coner.snoozle.db.sample

import org.coner.snoozle.db.entity.Entity
import org.coner.snoozle.db.Key
import java.util.*

data class Subwidget(
        val id: UUID = UUID.randomUUID(),
        val widgetId: UUID,
        val name: String
) : Entity<SubwidgetKey> {

    override val key by lazy { SubwidgetKey(id = id, widgetId = id) }
}

data class SubwidgetKey(
        val id: UUID,
        val widgetId: UUID
) : Key
