package org.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.snoozle.db.entity.EntityResource
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.util.readText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.nio.file.Path
import kotlin.streams.toList

class SubwidgetIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase
    private lateinit var resource: EntityResource<Subwidget.Key, Subwidget>

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
        resource = database.entity()
    }

    @Test
    fun `It should read a Subwidget`() {
        val widgetOneSubwidgetOne = SampleDb.Subwidgets.WidgetOneSubwidgetOne
        val key = Subwidget.Key(
                widgetId = widgetOneSubwidgetOne.widgetId,
                id = widgetOneSubwidgetOne.id
        )

        val actual = resource.read(key)

        assertThat(actual).isEqualTo(widgetOneSubwidgetOne)
    }

    @Test
    fun `It should create a Subwidget`() {
        val subwidget = Subwidget(
                widgetId = SampleDb.Widgets.Two.id,
                name = "Widget Two Subwidget Two"
        )

        resource.create(subwidget)

        val expectedFile = SampleDb.Subwidgets.tempFile(root, subwidget)
        Assertions.assertThat(expectedFile).exists()
        val expectedJson = SampleDb.Subwidgets.asJson(subwidget)
        val actualJson = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should update a Subwidget`() {
        val widgetOneSubwidgetOne = SampleDb.Subwidgets.WidgetOneSubwidgetOne
        val expectedFile = SampleDb.Subwidgets.tempFile(root, widgetOneSubwidgetOne)
        Assumptions.assumeThat(expectedFile).exists()
        val update = widgetOneSubwidgetOne.copy(
                name = "Updated"
        )
        val beforeJson = expectedFile.readText()
        val expectedJson = SampleDb.Subwidgets.asJson(update)

        resource.update(update)

        val actualJson = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
        JSONAssert.assertNotEquals(beforeJson, actualJson, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should delete Subwidget`() {
        val widgetOneSubwidgetOne = SampleDb.Subwidgets.WidgetOneSubwidgetOne
        val actualFile = SampleDb.Subwidgets.tempFile(root, widgetOneSubwidgetOne)
        Assumptions.assumeThat(actualFile).exists()

        resource.delete(widgetOneSubwidgetOne)

        Assertions.assertThat(actualFile).doesNotExist()
    }

    @Test
    fun `It should stream Subwidgets`() {
        val actual = resource.stream()
                .toList()
                .sortedBy { it.name }

        assertThat(actual).all {
            hasSize(2)
            index(0).isEqualTo(SampleDb.Subwidgets.WidgetOneSubwidgetOne)
            index(1).isEqualTo(SampleDb.Subwidgets.WidgetTwoSubwidgetOne)

        }
    }

}