package org.coner.snoozle.db

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.Widget
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.reflect.KClass

class DatabaseTest {

    @MockK
    lateinit var widgetResource: Resource<Widget>
    @MockK
    lateinit var subwidgetResource: Resource<Subwidget>

    lateinit var resources: Map<KClass<Entity>, Resource<Entity>>
    lateinit var database: Database

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxed = true)
        resources = mapOf(
                Widget::class as KClass<Entity> to widgetResource as Resource<Entity>,
                Subwidget::class as KClass<Entity> to subwidgetResource as Resource<Entity>
        )
        database = Database(resources)
    }

    @Test
    fun itShouldGet() {
        val widget: Widget = SampleDb.Widgets.One
        every { widgetResource.get(Widget::id to widget.id) }.returns(widget)

        val actual = database.get(Widget::id to widget.id)

        verify { widgetResource.get(Widget::id to widget.id) }
        assertThat(actual).isSameAs(widget)
    }

    @Test
    fun itShouldPut() {
        database.put(SampleDb.Widgets.One)

        verify { widgetResource.put(SampleDb.Widgets.One) }
    }

    @Test
    fun itShouldRemove() {
        database.remove(SampleDb.Widgets.One)

        verify { widgetResource.delete(SampleDb.Widgets.One) }
    }

    @Test
    fun itShouldListWidgets() {
        val widgets = listOf(
                SampleDb.Widgets.One,
                SampleDb.Widgets.Two
        )
        every { widgetResource.list() }.returns(widgets)

        val actual: List<Widget> = database.list()

        verify { widgetResource.list() }
        assertThat(actual).isSameAs(widgets)
    }

    @Test
    fun itShouldListSubwidgets() {
        val subwidgets = listOf(SampleDb.Subwidgets.WidgetTwoSubwidgetOne)
        val widgetId = subwidgets.first().widgetId

        every { subwidgetResource.list(Subwidget::widgetId to widgetId) }.returns(subwidgets)

        val actual = database.list(Subwidget::widgetId to widgetId)

        verify { subwidgetResource.list(Subwidget::widgetId to widgetId) }
        assertThat(actual).isSameAs(subwidgets)

    }


}