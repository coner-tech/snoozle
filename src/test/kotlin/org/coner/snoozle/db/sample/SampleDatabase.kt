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

    override val entities = entities {
        entity<Widget> {
            path = "widgets" / { it.id }
        }
        entity<Subwidget> {
            path = "widgets" / { it.widgetId } / "subwidgets" / { it.id }
        }
        entity<Gadget> {
            path = "gadgets" / { it.id }
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