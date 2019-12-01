package org.coner.snoozle.db.sample

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.db.Database
import java.nio.file.Path

class SampleDatabase(
        root: Path,
        objectMapper: ObjectMapper
) : Database(
        root = root,
        objectMapper = objectMapper
) {

    override val resources = resources {
        entity<Widget> {
            path = "widgets" / Widget::id
        }
        entity<Subwidget> {
            path = "widgets" / Subwidget::widgetId / "subwidgets" / Subwidget::id
        }
        entity<Gadget> {
            path = "gadgets" / Gadget::id
            versioning = EntityVersioningStrategy.AutomaticInternalVersioning
        }
        blob<Foo> {
            path = "foos" / Foo::id
            extension = ".foo"
        }
    }
}