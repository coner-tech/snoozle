package org.coner.snoozle.db.sample

import org.coner.snoozle.db.CurrentVersionRecord
import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.HistoricVersionRecord
import org.coner.snoozle.db.WholeRecord
import org.coner.snoozle.util.snoozleJacksonObjectMapper
import org.coner.snoozle.util.uuid
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object SampleDb {

    private val resourceUri = Paths.get(javaClass.getResource("/sample-db").toURI())

    fun factory(root: Path): SampleDatabase {
        resourceUri.toFile().copyRecursively(root.toFile())
        return SampleDatabase(
                root = root
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

        override fun tempFile(root: Path, entity: Widget): Path {
            return root.resolve("widgets/${entity.id}.json")
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

        override fun tempFile(root: Path, entity: Subwidget): Path {
            return root.resolve("widgets/${entity.widgetId}/subwidgets/${entity.id}.json")
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
                    name = "Gadget One",
                    silly = null
            )

        val GadgetOneWholeRecord
            get() = WholeRecord<Gadget>(
                    entityValue = GadgetOne,
                    currentVersion = CurrentVersionRecord(
                            version = 2,
                            ts = ZonedDateTime.parse("2019-05-16T21:43:00-05:00")
                    ),
                    history = listOf(
                            HistoricVersionRecord(
                                    version = 0,
                                    ts = ZonedDateTime.parse("2019-05-16T21:40:00-05:00"),
                                    entityValue = Gadget(
                                            id = GadgetOne.id,
                                            name = null,
                                            silly = null
                                    )
                            ),
                            HistoricVersionRecord(
                                    version = 1,
                                    ts = ZonedDateTime.parse("2019-05-16T21:42:00-05:00"),
                                    entityValue = Gadget(
                                            id = GadgetOne.id,
                                            name = "Gadget Won",
                                            silly = ZonedDateTime.parse("2019-05-16T21:41:59-05:00")
                                    )
                            )
                    )
            )

        override fun tempFile(root: Path, entity: Gadget): Path {
            return root.resolve("gadgets/${entity.id}.json")
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
        fun tempFile(root: Path, entity: E): Path
        fun asJson(entity: E): String
    }
}