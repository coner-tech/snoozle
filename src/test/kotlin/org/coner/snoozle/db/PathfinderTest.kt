package org.coner.snoozle.db

import org.assertj.core.api.Assertions
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.Widget
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths

class PathfinderTest {

    @TempDir
    lateinit var root: Path
    lateinit var widgetPathfinder: Pathfinder<Widget.Key, Widget>
    lateinit var subwidgetPathfinder: Pathfinder<Subwidget.Key, Subwidget>

    @BeforeEach
    fun before() {
        widgetPathfinder = Pathfinder(
                root = root,
                pathParts = listOf(
                        PathPart.StringValue("widgets"),
                        PathPart.DirectorySeparator(),
                        PathPart.UuidVariable { id },
                        PathPart.StringValue(".json")
                )
        )
        subwidgetPathfinder = Pathfinder(
                root = root,
                pathParts = listOf(
                        PathPart.StringValue("widgets"),
                        PathPart.DirectorySeparator(),
                        PathPart.UuidVariable { widgetId },
                        PathPart.DirectorySeparator(),
                        PathPart.StringValue("subwidgets"),
                        PathPart.DirectorySeparator(),
                        PathPart.UuidVariable { id },
                        PathPart.StringValue(".json")
                )
        )
    }

    @Test
    fun `It should find path for Widget by key`() {
        val widget = SampleDb.Widgets.One
        val key = Widget.Key(id = widget.id)

        val actual = widgetPathfinder.findRecord(key)

        val expected = Paths.get("widgets/${widget.id}.json")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun `It should find path for Subwidget by key`() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne
        val key = Subwidget.Key(widgetId = subwidget.widgetId, id = subwidget.id)

        val actual = subwidgetPathfinder.findRecord(key)

        val expected = Paths.get("widgets/${subwidget.widgetId}/subwidgets/${subwidget.id}.json")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun `It should find correct Widget candidate path is a record`() {
        TODO()
    }

    @Test
    fun `It should find incorrect Widget candidate path is not a record`() {
        TODO()
    }

    @Test
    fun `It should find correct Subwidget candidate path is a record`() {
        TODO()
    }

    @Test
    fun `It should find incorrect Subwidget candidate path is not a record`() {
        TODO()
    }

    @Test
    fun `It should find variable string parts from Widget path`() {
        TODO()
    }

    @Test
    fun `It should find variable string parts from Subwidget path`() {
        TODO()
    }
}