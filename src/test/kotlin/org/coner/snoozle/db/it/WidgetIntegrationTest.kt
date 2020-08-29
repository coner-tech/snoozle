package org.coner.snoozle.db.it

import assertk.all
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.snoozle.db.entity.EntityResource
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Widget
import org.coner.snoozle.util.readText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.nio.file.Path
import kotlin.streams.toList

class WidgetIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase
    private lateinit var resource: EntityResource<Widget.Key, Widget>

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
        resource = database.entity()
    }

    @Test
    fun `It should get a Widget`() {
        val widget = SampleDb.Widgets.One
        val key = Widget.Key(id = widget.id)

        val actual = resource.read(key)

        assertk.assertThat(actual).isEqualTo(widget)
    }

    @Test
    fun `It should create a Widget`() {
        val widget = Widget(name = "Create a Widget")

        resource.create(widget)

        val expectedFile = SampleDb.Widgets.tempFile(root, widget)
        val expectedJson = SampleDb.Widgets.asJson(widget)
        Assertions.assertThat(expectedFile).exists()
        val actual = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should delete a Widget`() {
        val widget = SampleDb.Widgets.One
        val actualFile = SampleDb.Widgets.tempFile(root, widget)
        Assumptions.assumeThat(actualFile).exists()

        resource.delete(widget)

        Assertions.assertThat(actualFile).doesNotExist()
    }

    @Test
    fun `It should stream Widgets`() {
        val widgets = resource.stream()
                .toList()
                .sortedBy { it.name }

        assertk.assertThat(widgets).all {
            hasSize(2)
            index(0).isEqualTo(SampleDb.Widgets.One)
            index(1).isEqualTo(SampleDb.Widgets.Two)
        }
    }
}