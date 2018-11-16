package org.coner.snoozle.db

import java.util.*

@EntityPath("/widgets/{widgetId}/subwidgets/{id}")
data class Subwidget(
        val id: UUID,
        val widgetId: UUID,
        val name: String
) : Entity