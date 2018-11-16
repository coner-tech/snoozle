package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.EntityPath
import java.util.*

@EntityPath("/widgets/{widgetId}/subwidgets/{id}")
data class Subwidget(
        val id: UUID,
        val widgetId: UUID,
        val name: String
) : Entity