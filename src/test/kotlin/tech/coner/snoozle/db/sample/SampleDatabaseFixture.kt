package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.path.RelativePath
import tech.coner.snoozle.db.path.asAbsolute
import tech.coner.snoozle.db.path.asRelative
import tech.coner.snoozle.util.requireResourceAsFile
import tech.coner.snoozle.util.uuid
import java.nio.file.Path
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.Path

object SampleDatabaseFixture {

    const val VERSION_HIGHEST = 3

    fun factory(root: Path, version: Int?, populate: Boolean = true): SampleDatabase {
        javaClass.requireResourceAsFile("/sample-db/v$version")
            .apply { if (populate) { copyRecursively(root.toFile()) } }
        return SampleDatabase(root.asAbsolute())
    }

    object Widgets : SampleEntity<Widget.Key, Widget> {
        val One
            get() = Widget(
                id = uuid("1f30d7b6-0296-489a-9615-55868aeef78a"),
                name = "Widget One",
                widget = true
            )
        val Two
            get() = Widget(
                id = uuid("94aa3940-1183-4e91-b329-d9dc9c688540"),
                name = "Widget Two",
                widget = true
            )

        override fun tempFile(root: Path, entity: Widget): Path {
            return root.resolve("widgets/${entity.id}.json")
        }

        fun tempFile(root: Path, databaseVersion: Int?, widgetId: UUID): Path {
            return when (databaseVersion) {
                null -> arrayOf("widget", "$widgetId.json")
                else -> arrayOf("widgets", "$widgetId.json")
            }
                .fold(root) { acc, segment -> acc.resolve(segment) }
        }

        fun relativePath(widget: Widget): RelativePath {
            return Path("widgets", "${widget.id}.json").asRelative()
        }

        override fun asJson(entity: Widget): String {
            return """
                {
                    "id": "${entity.id}",
                    "name": "${entity.name}",
                    "widget": ${entity.widget}
                }
            """.trimIndent()
        }
    }

    object Subwidgets : SampleEntity<Subwidget.Key, Subwidget> {
        val WidgetOneSubwidgetOne
            get() = Subwidget(
                id = uuid("220460be-27d4-4e6d-8ac3-34cf5139b229"),
                widgetId = SampleDatabaseFixture.Widgets.One.id,
                name = "Widget One's Subwidget One"
            )
        val WidgetTwoSubwidgetOne
            get() = Subwidget(
                id = uuid("0dc69a13-b911-4c39-bf56-19fcdb7a8baf"),
                widgetId = SampleDatabaseFixture.Widgets.Two.id,
                name = "Widget Two's Subwidget One"
            )

        override fun tempFile(root: Path, entity: Subwidget): Path {
            return root.resolve("widgets/${entity.widgetId}/subwidgets/${entity.id}.json")
        }

        override fun asJson(entity: Subwidget): String {
            return """
                {
                    "id": "${entity.id}",
                    "widgetId": "${entity.widgetId}",
                    "name": "${entity.name}"
                }
            """.trimIndent()
        }
    }

    object Gadgets : SampleEntity<Gadget.Key, Gadget> {
        val GadgetOne
            get() = Gadget(
                id = uuid("3d34e72e-14a5-4ab6-9bda-3d9262799274"),
                name = "Gadget One",
                silly = null
            )
        val GadgetTwo
            get() = Gadget(
                id = uuid("ed98db81-0b00-4184-8bf4-37d129bdafb9"),
                name = "Gadget Two",
                silly = null
            )

        val all
            get() = listOf(
                GadgetOne,
                GadgetTwo
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
                {
                    "id": "${entity.id}",
                    "name": "${entity.name}",
                    "silly": $silly
                }
            """.trimIndent()
        }
    }

    private interface SampleEntity<K : tech.coner.snoozle.db.Key, E : Entity<K>> {
        fun tempFile(root: Path, entity: E): Path
        fun asJson(entity: E): String
    }

}