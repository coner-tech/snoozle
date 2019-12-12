package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Database
import org.coner.snoozle.db.EntityResource
import org.coner.snoozle.db.versioning.EntityVersioningStrategy
import java.nio.file.Path
import java.util.*

class SampleDatabase(root: Path) : Database(root) {

    override val entities = registerEntities {
        entity<Widget> {
            path = "widgets" / { it.id } + ".json"
        }
        entity<Subwidget> {
            path = "widgets" / { it.widgetId } / "subwidgets" / { it.id } + ".json"
        }
        entity<Gadget> {
            path = "gadgets" / { it.id } + ".json"
            versioning = EntityVersioningStrategy.AutomaticInternalVersioning
        }
    }
}

fun EntityResource<Widget>.getWidget(id: UUID) = get(id)
fun EntityResource<Subwidget>.getSubwidget(widgetId: UUID, id: UUID) = get(widgetId, id)
fun EntityResource<Gadget>.getGadget(id: UUID) = get(id)
fun EntityResource<Gadget>.getWholeGadgetRecord(id: UUID) = getWholeRecord(id)