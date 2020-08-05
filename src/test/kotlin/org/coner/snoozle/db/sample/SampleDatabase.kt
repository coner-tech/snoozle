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
            path = "widgets" / { id } + ".json"
        }
        entity<Subwidget> {
            path = "widgets" / { widgetId } / "subwidgets" / { id } + ".json"
        }
        entity<Gadget> {
            path = "gadgets" / { id } + ".json"
            versioning = EntityVersioningStrategy.AutomaticInternalVersioning
        }
        blob<GadgetPhoto> {
            path = "gadgets" / { gadgetId } / "photos" / string { id } + "." + string { extension }
            factory = GadgetPhoto.Factory()
        }
        blob<GadgetPhotoCitation> {
            path = "gadgets" / { gadgetId } / "photos" / "citations" / string { id } + ".citation"
            factory = GadgetPhotoCitation.Factory()
        }
    }
}

fun EntityResource<Widget>.getWidget(id: UUID) = get(id)
fun EntityResource<Subwidget>.getSubwidget(widgetId: UUID, id: UUID) = get(widgetId, id)
fun EntityResource<Gadget>.getGadget(id: UUID) = get(id)
fun EntityResource<Gadget>.getWholeGadgetRecord(id: UUID) = getWholeRecord(id)
fun BlobResource<GadgetPhoto>.getGadgetPhoto(gadgetId: UUID, id: String) = getAsInputStream(gadgetId, id)