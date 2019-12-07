package org.coner.snoozle.db.it

import assertk.all
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.getSubwidget
import org.coner.snoozle.util.readText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.nio.file.Path

class SubwidgetIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
    }

    @Test
    fun itShouldGetSubwidgets() {
        val subwidgets = arrayOf(
                SampleDb.Subwidgets.WidgetOneSubwidgetOne,
                SampleDb.Subwidgets.WidgetTwoSubwidgetOne
        )

        for (expected in subwidgets) {
            val actual = database.entity<Subwidget>().getSubwidget(
                    expected.widgetId,
                    expected.id
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

        database.entity<Subwidget>().put(subwidget)

        val expectedFile = SampleDb.Subwidgets.tempFile(root, subwidget)
        val expectedJson = """
            {
                ${SampleDb.Subwidgets.asJson(subwidget)}
            }
        """.trimIndent()
        Assertions.assertThat(expectedFile).exists()
        val actual = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldDeleteSubwidget() {
        val subwidgets = arrayOf(
                SampleDb.Subwidgets.WidgetOneSubwidgetOne,
                SampleDb.Subwidgets.WidgetTwoSubwidgetOne
        )

        for (subwidget in subwidgets) {
            val actualFile = SampleDb.Subwidgets.tempFile(root, subwidget)
            Assumptions.assumeThat(actualFile).exists()

            database.entity<Subwidget>().delete(subwidget)

            Assertions.assertThat(actualFile).doesNotExist()
        }
    }

    @Test
    fun itShouldListSubwidget() {
        val subwidgets = database.entity<Subwidget>().list(SampleDb.Widgets.One.id)

        assertk.assertThat(subwidgets).all {
            hasSize(1)
            index(0).isEqualTo(SampleDb.Subwidgets.WidgetOneSubwidgetOne)
        }
    }

}