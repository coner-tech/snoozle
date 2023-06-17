package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.session.data.DataSession
import java.util.*

data class Subwidget(
        val id: SubwidgetId = UUID.randomUUID(),
        val widgetId: WidgetId,
        val name: String
) : Entity<Subwidget.Key> {

    data class Key(
            val id: SubwidgetId,
            val widgetId: WidgetId
    ) : tech.coner.snoozle.db.Key

    override fun toKey() = Key(id = id, widgetId = widgetId)
}

typealias SubwidgetResource = EntityResource<Subwidget.Key, Subwidget>
typealias SubwidgetId = UUID
fun DataSession.subwidgets(): SubwidgetResource = entity()
fun SubwidgetResource.watchAllSubwidgets() = buildWatch { listOf(uuidIsAny(), uuidIsAny()) }
fun SubwidgetResource.watchSubwidget(subwidgetKey: Subwidget.Key) = buildWatch { listOf(uuidIsEqualTo(subwidgetKey.widgetId), uuidIsEqualTo(subwidgetKey.id)) }
fun SubwidgetResource.watchSubwidgetsOf(widgetKey: Widget.Key) = buildWatch { listOf(uuidIsEqualTo(widgetKey.id), uuidIsAny()) }
