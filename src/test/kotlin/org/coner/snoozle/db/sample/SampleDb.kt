package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Entity
import org.coner.snoozle.util.snoozleJacksonObjectMapper
import org.coner.snoozle.util.uuid
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.format.DateTimeFormatter

object SampleDb {

    private val resourceUri = File(javaClass.getResource("/sample-db").toURI())

    fun factory(temporaryFolder: TemporaryFolder): SampleDatabase {
        resourceUri.copyRecursively(temporaryFolder.root)
        return SampleDatabase(
                root = temporaryFolder.root,
                objectMapper = snoozleJacksonObjectMapper()
        )
    }

    object Widgets : SampleEntity<Widget> {
        val One
            get() = Widget(
                    id = uuid("1f30d7b6-0296-489a-9615-55868aeef78a"),
                    name = "Widget One"
            )
        val Two
            get() = Widget(
                    id = uuid("94aa3940-1183-4e91-b329-d9dc9c688540"),
                    name = "Widget Two"
            )

        override fun tempFile(temporaryFolder: TemporaryFolder, entity: Widget): File {
            return File(temporaryFolder.root, "/widgets/${entity.id}.json")
        }

        override fun asJson(entity: Widget): String {
            return """
                {
                    entity: {
                        id: "${entity.id}",
                        name: "${entity.name}"
                    }
                }
            """.trimIndent()
        }
    }

    object Subwidgets : SampleEntity<Subwidget> {
        val WidgetOneSubwidgetOne
                get() = Subwidget(
                        id = uuid("220460be-27d4-4e6d-8ac3-34cf5139b229"),
                        widgetId = SampleDb.Widgets.One.id,
                        name = "Widget One's Subwidget One"
                )
        val WidgetTwoSubwidgetOne
                get() = Subwidget(
                        id = uuid("0dc69a13-b911-4c39-bf56-19fcdb7a8baf"),
                        widgetId = SampleDb.Widgets.Two.id,
                        name = "Widget Two's Subwidget One"
                )

        override fun tempFile(temporaryFolder: TemporaryFolder, entity: Subwidget): File {
            return File(temporaryFolder.root, "/widgets/${entity.widgetId}/subwidgets/${entity.id}.json")
        }

        override fun asJson(entity: Subwidget): String {
            return """
                "entity": {
                    "id": "${entity.id}",
                    "widgetId": "${entity.widgetId}",
                    "name": "${entity.name}"
                }
            """.trimIndent()
        }
    }

    object Gadgets : SampleEntity<Gadget> {
        val GadgetOne
            get() = Gadget(
                    id = uuid("3d34e72e-14a5-4ab6-9bda-3d9262799274"),
                    name = "Gadget One"
            )

        override fun tempFile(temporaryFolder: TemporaryFolder, entity: Gadget): File {
            return File(temporaryFolder.root, "/gadgets/${entity.id}.json")
        }

        override fun asJson(entity: Gadget): String {
            val silly = if (entity.silly != null) {
                DateTimeFormatter.ISO_INSTANT.format(entity.silly)
                        .padStart(1, '"')
                        .padEnd(1, '"')
            } else {
                null
            }
            return """
                "entity": {
                    "id": "${entity.id}",
                    "name": "${entity.name}",
                    "silly": $silly
                }
            """.trimIndent()
        }
    }

    private interface SampleEntity<E : Entity> {
        fun tempFile(temporaryFolder: TemporaryFolder, entity: E): File
        fun asJson(entity: E): String
    }
}