package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.Database
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.migration.MigrationPathMatcher
import tech.coner.snoozle.db.path.AbsolutePath
import tech.coner.snoozle.db.session.data.DataSession
import java.util.*

class SampleDatabase(root: AbsolutePath) : Database(root) {

    override val version: Int = 3
    override val types = registerTypes {
        entity<Widget.Key, Widget> {
            path = "widgets" / { id } + ".json"
            keyFromPath = { Widget.Key(id = uuidAt(0)) }
            keyFromEntity = { Widget.Key(id = id) }
        }
        entity<Subwidget.Key, Subwidget> {
            path = "widgets" / { widgetId } / "subwidgets" / { id } + ".json"
            keyFromPath = { Subwidget.Key(widgetId = uuidAt(0), id = uuidAt(1)) }
            keyFromEntity = { Subwidget.Key(widgetId = widgetId, id = id) }
        }
        entity<Gadget.Key, Gadget> {
            path = "gadgets" / { id } + ".json"
            keyFromPath = { Gadget.Key(uuidAt(0)) }
            keyFromEntity = { Gadget.Key(id = id) }
        }
        blob<GadgetPhoto> {
            path = "gadgets" / { gadgetId } / "photos" / string { id } + "." + string { extension }
            keyFromPath = { GadgetPhoto(gadgetId = uuidAt(0), id = stringAt(1), extension = stringAt(2)) }
        }
        blob<GadgetPhotoCitation> {
            path = "gadgets" / { gadgetId } / "photos" / "citations" / string { id } + ".citation"
            keyFromPath = { GadgetPhotoCitation(gadgetId = uuidAt(0), id = stringAt(1)) }
        }
    }
    override val migrations = registerMigrations {
        migrate(null to 1) {
            val widgets: List<MigrationPathMatcher> = "widgets" / matchUuid() + ".json"
            move(
                from = "widget" / matchUuid() + ".json",
                to = widgets
            )
            move(
                from = "widget" / matchUuid() / "subwidget" / matchUuid() + ".json",
                to = "widgets" / matchUuid() / "subwidgets" / matchUuid() + ".json"
            )
            deleteDirectories("widget" / matchUuid() / "subwidget")
            deleteDirectories("widget" / matchUuid())
            deleteDirectory("widget")
            onEntities(widgets) {
                addBooleanProperty("boolean", false)
                addBooleanProperty("nullBoolean", null)
                addStringProperty("string", "string")
                addStringProperty("nullString", null)
                addIntProperty("int", 0)
                addIntProperty("nullInt", null as Int?)
                addFloatProperty("nullFloat", null)
                addFloatProperty("float", 1.1f)
                addDoubleProperty("double", 1.2345678912345679E8)
                addDoubleProperty("nullDouble", null)
                addLongProperty("long", 123456789)
                addLongProperty("nullLong", null)
                addShortProperty("short", 0)
                addShortProperty("nullShort", null)
                addArray("array")
                addObject("object")
                addNull("null")
                onNode("/object") {
                    addArray("array")
                }
            }
        }
        migrate(1 to 2) {
            val widgets: List<MigrationPathMatcher> = "widgets" / matchUuid() + ".json"
            onEntities(widgets) {
                onNode("/object") {
                    onArrayObjects("array") {
                        addBooleanProperty("boolean", true)
                    }
                }
            }
        }
        migrate(2 to 3) {
            val widgets: List<MigrationPathMatcher> = "widgets" / matchUuid() + ".json"
            onEntities(widgets) {
                addBooleanProperty(name = "widget", defaultValue = true)
                removeProperty(name = "notWidget")
                removeProperties(
                    "boolean",
                    "nullBoolean",
                    "double",
                    "nullDouble",
                    "string",
                    "nullString",
                    "int",
                    "nullInt",
                    "float",
                    "nullFloat",
                    "double",
                    "nullDouble",
                    "long",
                    "nullLong",
                    "short",
                    "nullShort",
                    "array",
                    "object",
                    "null"
                )
            }
        }
    }
}

typealias WidgetResource = EntityResource<Widget.Key, Widget>
fun DataSession.widgets(): WidgetResource = entity()
fun WidgetResource.watchAllWidgets() = buildWatch { listOf(uuidIsAny()) }
fun WidgetResource.watchWidget(widgetId: UUID) = buildWatch { listOf(uuidIsEqualTo(widgetId)) }
fun WidgetResource.watchWidgets(widgetIds: Collection<UUID>) = buildWatch { listOf(uuidIsOneOf(widgetIds)) }

typealias SubwidgetResource = EntityResource<Subwidget.Key, Subwidget>
fun DataSession.subwidgets(): SubwidgetResource = entity()
fun SubwidgetResource.watchAllSubwidgets() = buildWatch { listOf(uuidIsAny(), uuidIsAny()) }
fun SubwidgetResource.watchSubwidget(widgetId: UUID, subwidgetId: UUID) = buildWatch { listOf(uuidIsEqualTo(widgetId), uuidIsEqualTo(subwidgetId)) }
fun SubwidgetResource.watchSubwidgetsOf(widgetId: UUID) = buildWatch { listOf(uuidIsEqualTo(widgetId), uuidIsAny()) }
