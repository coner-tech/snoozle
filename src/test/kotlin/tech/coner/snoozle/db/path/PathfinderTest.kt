package tech.coner.snoozle.db.path

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.sample.Subwidget
import tech.coner.snoozle.db.sample.Widget
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
                root = root.asAbsolute(),
                pathParts = listOf(
                        PathPart.StringValue("widgets"),
                        PathPart.DirectorySeparator(),
                        PathPart.UuidVariable { id },
                        PathPart.StringValue(".json")
                )
        )
        subwidgetPathfinder = Pathfinder(
                root = root.asAbsolute(),
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
        val widget = SampleDatabaseFixture.Widgets.One
        val key = Widget.Key(id = widget.id)

        val actual = widgetPathfinder.findRecord(key)

        val expected = Paths.get("widgets", "${widget.id}.json")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `It should find path for Subwidget by key`() {
        val subwidget = SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne
        val key = Subwidget.Key(widgetId = subwidget.widgetId, id = subwidget.id)

        val actual = subwidgetPathfinder.findRecord(key)

        val expected = Paths.get("widgets", subwidget.widgetId.toString(), "subwidgets", "${subwidget.id}.json")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `It should find correct Widget candidate path is a record`() {
        val correctWidgetCandidate = root.relativize(SampleDatabaseFixture.Widgets.tempFile(root, SampleDatabaseFixture.Widgets.One))
            .asRelative()

        val actual = widgetPathfinder.isRecord(correctWidgetCandidate)

        assertThat(actual).isTrue()
    }

    @Test
    fun `It should find correct Subwidget candidate path is a record`() {
        val correctSubwidgetCandidate = root.relativize(SampleDatabaseFixture.Subwidgets.tempFile(root, SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne))
            .asRelative()

        val actual = subwidgetPathfinder.isRecord(correctSubwidgetCandidate)

        assertThat(actual).isTrue()
    }

    @Test
    fun `It should find variable string parts from Widget path`() {
        val widgetOne = SampleDatabaseFixture.Widgets.One
        val relativeRecordPath = Paths.get("widgets", "${widgetOne.id}.json").asRelative()
        val expected = arrayOf(widgetOne.id.toString())

        val actual = widgetPathfinder.findVariableStringParts(relativeRecordPath)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `It should find variable string parts from Subwidget path`() {
        val subwidget = SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne
        val relativeRecordPath = Paths.get("widgets", "${subwidget.widgetId}", "subwidgets", "${subwidget.id}.json").asRelative()
        val expected = arrayOf(
                "${subwidget.widgetId}",
                "${subwidget.id}"
        )

        val actual = subwidgetPathfinder.findVariableStringParts(relativeRecordPath)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `It should return an empty stream when no records exist`() {
        val actual = widgetPathfinder.streamAll()
        
        Assertions.assertThat(actual).isEmpty()
    }
}