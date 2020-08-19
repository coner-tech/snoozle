package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Database
import java.nio.file.Path

class SampleDatabase(root: Path) : Database(root) {

    override val types = registerTypes {
        entity<Widget, WidgetKey> {
            path = "widgets" / { id } + ".json"
            entityKey = { WidgetKey(id = uuidAt(0)) }
        }
        entity<Subwidget, SubwidgetKey> {
            path = "widgets" / { widgetId } / "subwidgets" / { id } + ".json"
            entityKey = { SubwidgetKey(widgetId = uuidAt(0), id = uuidAt(1)) }
        }
        versionedEntity<Gadget, GadgetKey> {
            path = "gadgets" / { id } / version + ".json"
            versionedEntityKeyParser { GadgetKey(id = uuidAt(0)) }
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
