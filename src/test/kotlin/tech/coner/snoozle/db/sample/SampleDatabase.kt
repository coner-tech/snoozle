package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.Database
import java.nio.file.Path

class SampleDatabase(root: Path) : tech.coner.snoozle.db.Database(root) {

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
}
