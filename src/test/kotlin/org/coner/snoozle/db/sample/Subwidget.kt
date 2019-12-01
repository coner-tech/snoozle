package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.EntityPath
import java.util.*

data class Subwidget(
        val id: UUID = UUID.randomUUID(),
        val widgetId: UUID,
        val name: String
) : Entity