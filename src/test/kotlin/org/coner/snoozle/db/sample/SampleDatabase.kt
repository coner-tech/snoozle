package org.coner.snoozle.db.sample

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.db.Database
import java.io.File

class SampleDatabase(
        root: File,
        objectMapper: ObjectMapper
) : Database(
        root = root,
        objectMapper = objectMapper) {

    override val entities = listOf(
            entityDefinition<Widget>(),
            entityDefinition<Subwidget>(),
            entityDefinition<Gadget>()
    )
}