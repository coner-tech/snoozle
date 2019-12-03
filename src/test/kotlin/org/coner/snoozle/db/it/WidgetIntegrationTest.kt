package org.coner.snoozle.db.it

import assertk.all
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Widget
import org.coner.snoozle.db.sample.getWidget
import org.coner.snoozle.util.readText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.nio.file.Path

class WidgetIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
    }

    @Test
    fun itShouldGetWidgets() {
        val widgets = arrayOf(SampleDb.Widgets.One, SampleDb.Widgets.Two)

        for (expected in widgets) {
            val actual = database.entity<Widget>().getWidget(expected.id)

            assertk.assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun itShouldPutWidget() {
        val widget = Widget(name = "Put Widget")

        database.entity<Widget>.put(widget)

        val expectedFile = SampleDb.Widgets.tempFile(root, widget)
        val expectedJson = SampleDb.Widgets.asJson(widget)
        Assertions.assertThat(expectedFile).exists()
        val actual = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldRemoveWidget() {
        val widgets = arrayOf(SampleDb.Widgets.One, SampleDb.Widgets.Two)

        for (widget in widgets) {
            val actualFile = SampleDb.Widgets.tempFile(root, widget)
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