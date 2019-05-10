package org.coner.snoozle.db.it

import assertk.all
import assertk.assertions.exists
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Widget
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class WidgetIntegrationTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private lateinit var database: SampleDatabase

    @Before
    fun before() {
        database = SampleDb.factory(folder)
    }

    @Test
    fun itShouldGetWidgets() {
        val widgets = arrayOf(SampleDb.Widgets.One, SampleDb.Widgets.Two)

        for (expected in widgets) {
            val actual = database.get(Widget::id to expected.id)

            assertk.assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun itShouldPutWidget() {
        val widget = Widget(name = "Put Widget")

        database.put(widget)

        val expectedFile = SampleDb.Widgets.tempFile(folder, widget)
        val expectedJson = SampleDb.Widgets.asJson(widget)
        assertk.assertThat(expectedFile).exists()
        JSONAssert.assertEquals(expectedJson, expectedFile.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldRemoveWidget() {
        val widgets = arrayOf(SampleDb.Widgets.One, SampleDb.Widgets.Two)

        for (widget in widgets) {
            val actualFile = SampleDb.Widgets.tempFile(folder, widget)
            Assumptions.assumeThat(actualFile).exists()

            database.remove(widget)

            Assertions.assertThat(actualFile).doesNotExist()
        }
    }

    @Test
    fun itShouldListWidget() {
        val widgets: List<Widget> = database.list()

        assertk.assertThat(widgets).all {
            hasSize(2)
            index(0).isEqualTo(SampleDb.Widgets.One)
            index(1).isEqualTo(SampleDb.Widgets.Two)
        }
    }
}