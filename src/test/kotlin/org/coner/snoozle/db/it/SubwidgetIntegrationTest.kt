package org.coner.snoozle.db.it

import com.gregwoodfill.assert.shouldEqualJson
import org.assertj.core.api.Assertions.assertThat
import org.coner.snoozle.db.Database
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.Widget
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.*

class SubwidgetIntegrationTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()

    @Before
    fun before() {
        SampleDb.copyTo(folder)
    }

    @Test
    fun itShouldGetSubwidgetById() {
        val database = Database(folder.root, Widget::class, Subwidget::class)
        val expected = SampleDb.Subwidgets.WidgetOneSubwidgetOne

        val actual = database.get(
                Subwidget::widgetId to SampleDb.Widgets.One.id,
                Subwidget::id to SampleDb.Subwidgets.WidgetOneSubwidgetOne.id
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun itShouldPutSubwidgetById() {
        val database = Database(folder.root, Widget::class, Subwidget::class)
        val subwidget = Subwidget(
                id = UUID.randomUUID(),
                widgetId = SampleDb.Widgets.Two.id,
                name = "Widget Two's Subwidget Two"
        )

        database.put(subwidget)

        val record = SampleDb.Subwidgets.tempFile(folder, subwidget)
        assertThat(record).exists()
        val actual = record.readText()
        actual.shouldEqualJson("""
            {
                "id":"${subwidget.id}",
                "widgetId":"${SampleDb.Widgets.Two.id}",
                "name":"Widget Two's Subwidget Two"
            }
        """)
    }

    @Test
    fun itShouldRemoveEntity() {
        val database = Database(folder.root, Widget::class, Subwidget::class)
        val subwidget = SampleDb.Subwidgets.WidgetOneSubwidgetOne
        val file = SampleDb.Subwidgets.tempFile(folder, subwidget)
        assertThat(file).exists() // sanity check

        database.remove(subwidget)

        assertThat(file).doesNotExist()
    }

    @Test
    fun itShouldList() {
        val database = Database(folder.root, Widget::class, Subwidget::class)
        val expected = listOf(SampleDb.Subwidgets.WidgetOneSubwidgetOne)

        val actual = database.list(Subwidget::widgetId to SampleDb.Subwidgets.WidgetOneSubwidgetOne.widgetId)

        assertThat(actual).isEqualTo(expected)
    }
}