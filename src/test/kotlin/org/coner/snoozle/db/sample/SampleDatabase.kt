package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Database
import org.coner.snoozle.db.blob.BlobResource
import org.coner.snoozle.db.entity.EntityResource
import org.coner.snoozle.db.versioning.EntityVersioningStrategy
import org.coner.snoozle.util.nameWithoutExtension
import org.coner.snoozle.util.uuid
import java.nio.file.Path
import java.util.*

class SampleDatabase(root: Path) : Database(root) {

    override val types = registerTypes {
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
        blob<GadgetPhoto> {
            path = "gadgets" / { it.gadgetId } / "photos" / string { it.id } + "." + string { it.extension }
            factory = GadgetPhoto.Factory()
        }
        blob<GadgetPhotoCitation> {
            path = "gadgets" / { it.gadgetId } / "photos" / "citations" / string { it.id } + ".citation"
            factory = GadgetPhotoCitation.Factory()
        }
    }
}

fun EntityResource<Widget>.getWidget(id: UUID) = get(id)
fun EntityResource<Subwidget>.getSubwidget(widgetId: UUID, id: UUID) = get(widgetId, id)
fun EntityResource<Gadget>.getGadget(id: UUID) = get(id)
fun EntityResource<Gadget>.getWholeGadgetRecord(id: UUID) = getWholeRecord(id)
fun BlobResource<GadgetPhoto>.getGadgetPhoto(gadgetId: UUID, id: String) = getAsInputStream(gadgetId, id)