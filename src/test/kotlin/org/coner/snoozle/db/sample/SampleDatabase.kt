package org.coner.snoozle.db.sample

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.db.Database
import org.coner.snoozle.db.EntityResource
import org.coner.snoozle.db.versioning.EntityVersioningStrategy
import java.nio.file.Path
import java.util.*

class SampleDatabase(
        root: Path,
        objectMapper: ObjectMapper
) : Database(
        root = root,
        objectMapper = objectMapper
) {

    override val entities = registerEntity {
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

//    override val blobs = blobs {
//        blob<Foo> {
//            path = "foos" / { it.id },
//            extension = ".foo"
//        }
//    }
}

fun EntityResource<Widget>.getWidget(id: UUID) = get(id)
fun EntityResource<Subwidget>.getSubwidget(widgetId: UUID, id: UUID) = get(widgetId, id)
fun EntityResource<Gadget>.getGadget(id: UUID) = get(id)
fun EntityResource<Gadget>.getWholeGadgetRecord(id: UUID) = getWholeRecord(id)