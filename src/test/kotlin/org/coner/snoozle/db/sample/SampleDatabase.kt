package org.coner.snoozle.db.sample

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.db.Database
import org.coner.snoozle.db.EntityResource
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
            path = "widgets" / { it.id } / ".json"
        }
        entity<Subwidget> {
            path = "widgets" / { it.widgetId } / "subwidgets" / { it.id } / ".json"
        }
        entity<Gadget> {
            path = "gadgets" / { it.id } / ".json"
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