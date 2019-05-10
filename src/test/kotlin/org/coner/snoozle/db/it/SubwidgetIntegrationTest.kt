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
import org.coner.snoozle.db.sample.Subwidget
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class SubwidgetIntegrationTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private lateinit var database: SampleDatabase

    @Before
    fun before() {
        database = SampleDb.factory(folder)
    }

    @Test
    fun itShouldGetSubwidgets() {
        val subwidgets = arrayOf(
                SampleDb.Subwidgets.WidgetOneSubwidgetOne,
                SampleDb.Subwidgets.WidgetTwoSubwidgetOne
        )

        for (expected in subwidgets) {
            val actual = database.get(
                    Subwidget::widgetId to expected.widgetId,
                    Subwidget::id to expected.id
            )

            assertk.assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun itShouldPutSubwidget() {
        val subwidget = Subwidget(
                widgetId = SampleDb.Widgets.Two.id,
                name = "Widget Two Subwidget Two"
        )

        database.put(subwidget)

        val expectedFile = SampleDb.Subwidgets.tempFile(folder, subwidget)
        val expectedJson = SampleDb.Subwidgets.asJson(subwidget)
        assertk.assertThat(expectedFile).exists()
        JSONAssert.assertEquals(expectedJson, expectedFile.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldRemoveSubwidget() {
        val subwidgets = arrayOf(
                SampleDb.Subwidgets.WidgetOneSubwidgetOne,
                SampleDb.Subwidgets.WidgetTwoSubwidgetOne
        )

        for (subwidget in subwidgets) {
            val actualFile = SampleDb.Subwidgets.tempFile(folder, subwidget)
            Assumptions.assumeThat(actualFile).exists()

            database.remove(subwidget)

            Assertions.assertThat(actualFile).doesNotExist()
        }
    }

    @Test
    fun itShouldListSubwidget() {
        val subwidgets: List<Subwidget> = database.list(
                Subwidget::widgetId to SampleDb.Widgets.One.id
        )

        assertk.assertThat(subwidgets).all {
            hasSize(1)
            index(0).isEqualTo(SampleDb.Subwidgets.WidgetOneSubwidgetOne)
        }
    }

}