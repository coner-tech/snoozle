package org.coner.snoozle.db.path

import org.assertj.core.api.Assertions
import org.coner.snoozle.db.PathPart
import org.coner.snoozle.db.Pathfinder
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
    lateinit var temp: Path
    lateinit var widgetPathfinder: Pathfinder<Widget>
    lateinit var subwidgetPathfinder: Pathfinder<Subwidget>

    @BeforeEach
    fun before() {
        widgetPathfinder = Pathfinder(
                root = temp,
                pathParts = listOf(
                        PathPart.StringValue("widgets"),
                        PathPart.DirectorySeparator(),
                        PathPart.UuidVariable { id },
                        PathPart.StringValue(".json")
                )
        )
        subwidgetPathfinder = Pathfinder(
                root = temp,
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
    fun itShouldFindPathForWidgetByArgs() {
        val widget = SampleDb.Widgets.One

        val actual = widgetPathfinder.findRecordByArgs(widget.id)

        val expected = Paths.get("widgets/${widget.id}.json")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindPathOfSubwidgetByArgs() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = subwidgetPathfinder.findRecordByArgs(
                subwidget.widgetId,
                subwidget.id
        )

        val expected = Paths.get("widgets/${subwidget.widgetId}/subwidgets/${subwidget.id}.json")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindPathListingOfWidgetByArgs() {
        val actual = widgetPathfinder.findListingByArgs()

        val expected = Paths.get("widgets/")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindPathListingOfSubwidgetByArgs() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = subwidgetPathfinder.findListingByArgs(subwidget.widgetId)

        val expected = Paths.get("widgets/${subwidget.widgetId}/subwidgets/")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindPathListingOfWidgetByRecordInstance() {
        val widget = SampleDb.Widgets.One

        val actual = widgetPathfinder.findListingByRecord(record = widget)

        val expected = Paths.get("widgets/")
        assertk.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun itShouldFindPathListingOfSubwidgetByRecordInstance() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = subwidgetPathfinder.findListingByRecord(record = subwidget)

        val expected = Paths.get("widgets/${subwidget.widgetId}/subwidgets/")
        assertk.assertThat(actual).isEqualTo(expected)
    }

}