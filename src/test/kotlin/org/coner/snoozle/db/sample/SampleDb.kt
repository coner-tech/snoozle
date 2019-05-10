package org.coner.snoozle.db.sample

import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*

object SampleDb {

    private val resourceUri = File(javaClass.getResource("/sample-db").toURI())

    fun factory(temporaryFolder: TemporaryFolder): SampleDatabase {
        resourceUri.copyRecursively(temporaryFolder.root)
        return SampleDatabase(root = temporaryFolder.root)
    }

    object Widgets {
        val One = Widget(
                id = UUID.fromString("1f30d7b6-0296-489a-9615-55868aeef78a"),
                name = "Widget One"
        )
        val Two = Widget(
                id = UUID.fromString("94aa3940-1183-4e91-b329-d9dc9c688540"),
                name = "Widget Two"
        )

        fun tempFile(temporaryFolder: TemporaryFolder, widget: Widget): File {
            return File(temporaryFolder.root, "/widgets/${widget.id}.json")
        }

        fun asJson(widget: Widget): String {
            return """
                {
                    entity: {
                        id: "${widget.id}",
                        name: "${widget.name}"
                    }
                }
            """.trimIndent()
        }
    }

    object Subwidgets {
        val WidgetOneSubwidgetOne = Subwidget(
                id = UUID.fromString("220460be-27d4-4e6d-8ac3-34cf5139b229"),
                widgetId = SampleDb.Widgets.One.id,
                name = "Widget One's Subwidget One"
        )
        val WidgetTwoSubwidgetOne = Subwidget(
                id = UUID.fromString("0dc69a13-b911-4c39-bf56-19fcdb7a8baf"),
                widgetId = SampleDb.Widgets.Two.id,
                name = "Widget Two's Subwidget One"
        )

        fun tempFile(temporaryFolder: TemporaryFolder, subwidget: Subwidget): File {
            return File(temporaryFolder.root, "/widgets/${subwidget.widgetId}/subwidgets/${subwidget.id}.json")
        }

        fun asJson(subwidget: Subwidget): String {
            return """
                {
                    "entity": {
                        "id": "${subwidget.id}",
                        "widgetId": "${subwidget.widgetId}",
                        "name": "${subwidget.name}"
                    }
                }
            """.trimIndent()
        }
    }
}