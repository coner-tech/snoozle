package org.coner.snoozle.db.sample

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.coner.snoozle.db.Database
import java.io.File

class SampleDatabase(
        root: File,
        objectMapper: ObjectMapper = jacksonObjectMapper()
) : Database(
        root = root,
        objectMapper = objectMapper) {

    override val entities = listOf(
            entityDefinition<Widget>(),
            entityDefinition<Subwidget>()
    )
}