package org.coner.snoozle.db

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
        widgetPathfinder = Pathfinder(Widget::class)
        subwidgetPathfinder = Pathfinder(Subwidget::class)
    }

    @Test
    fun itShouldFindEntityPathForWidgetIds() {
        val widget = SampleDb.Widgets.One

        val actual = widgetPathfinder.findEntity(Widget::id to widget.id)

        val expected = Paths.get("widgets/${widget.id}.json")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindEntityPathForSubwidgetIds() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = subwidgetPathfinder.findEntity(
                Subwidget::widgetId to subwidget.widgetId,
                Subwidget::id to subwidget.id
        )

        val expected = Paths.get("widgets/${subwidget.widgetId}/subwidgets/${subwidget.id}.json")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindParentOfEntityForWidget() {
        val widget = SampleDb.Widgets.One

        val actual = widgetPathfinder.findParentOfEntity(widget)

        val expected = Paths.get("widgets")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindParentOfEntityForSubwidget() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = subwidgetPathfinder.findParentOfEntity(subwidget)

        val expected = Paths.get("widgets/${subwidget.widgetId}/subwidgets")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindListingForWidget() {
        val actual = widgetPathfinder.findListing()

        val expected = Paths.get("widgets/")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

    @Test
    fun itShouldFindListingForSubwidget() {
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = subwidgetPathfinder.findListing(Subwidget::widgetId to subwidget.widgetId)

        val expected = Paths.get("widgets/${subwidget.widgetId}/subwidgets/")
        Assertions.assertThat(actual)
                .isRelative()
                .isEqualTo(expected)
    }

}