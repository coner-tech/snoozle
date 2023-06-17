package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.session.data.DataSession
import java.util.*

data class Widget(
        val id: WidgetId = UUID.randomUUID(),
        val name: String,
        val widget: Boolean
) : Entity<Widget.Key> {

    data class Key(val id: WidgetId) : tech.coner.snoozle.db.Key

    override fun toKey() = Key(id = id)
}

typealias WidgetResource = EntityResource<Widget.Key, Widget>
typealias WidgetId = UUID
fun DataSession.widgets(): WidgetResource = entity()
fun WidgetResource.watchAllWidgets() = buildWatch { listOf(uuidIsAny()) }
fun WidgetResource.watchWidget(widgetKey: Widget.Key) = buildWatch { listOf(uuidIsEqualTo(widgetKey.id)) }
fun WidgetResource.watchWidgets(widgetKeys: Collection<Widget.Key>) = buildWatch { listOf(uuidIsOneOf(widgetKeys.map { it.id })) }
