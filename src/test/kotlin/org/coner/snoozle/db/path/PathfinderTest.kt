package org.coner.snoozle.db.path

import assertk.assertions.isEqualTo
import org.assertj.core.api.Assertions
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.Widget
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class PathfinderTest {

    lateinit var widgetPathfinder: Pathfinder<Widget>
    lateinit var subwidgetPathfinder: Pathfinder<Subwidget>

    @BeforeEach
    fun before() {
        widgetPathfinder = Pathfinder(
                pathParts = listOf(
                        PathPart.StringPathPart("widgets"),
                        PathPart.DirectorySeparatorPathPart(),
                        PathPart.UuidPathPart { it.id},
                        PathPart.StringPathPart(".json")
                )
        )
        subwidgetPathfinder = Pathfinder(
                pathParts = listOf(
                        PathPart.StringPathPart("widgets"),
                        PathPart.DirectorySeparatorPathPart(),
                        PathPart.UuidPathPart { it.widgetId },
                        PathPart.DirectorySeparatorPathPart(),
                        PathPart.StringPathPart("subwidgets"),
                        PathPart.DirectorySeparatorPathPart(),
                        PathPart.UuidPathPart { it.id },
                        PathPart.StringPathPart(".json")
                )
        )
    }

    @Test
    fun itShouldFindPathForWidgetByArgs() {
        val widget = SampleDb.Widgets.One

        val actual = widgetPathfinder.findRecord(widget.id)

        val expected = Paths.get("widgets/${widget.id}.json")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindPathOfSubwidgetByArgs() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = subwidgetPathfinder.findRecord(
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
        val actual = widgetPathfinder.findListing()

        val expected = Paths.get("widgets/")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindPathListingOfSubwidgetByArgs() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = subwidgetPathfinder.findListing(subwidget.widgetId)

        val expected = Paths.get("widgets/${subwidget.widgetId}/subwidgets/")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindPathListingOfWidgetByRecordInstance() {
        val widget = SampleDb.Widgets.One

        val actual = widgetPathfinder.findListing(record = widget)

        val expected = Paths.get("widgets/")
        assertk.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun itShouldFindPathListingOfSubwidgetByRecordInstance() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = subwidgetPathfinder.findListing(record = subwidget)

        val expected = Paths.get("widgets/${subwidget.widgetId}/subwidgets/")
        assertk.assertThat(actual).isEqualTo(expected)
    }

}