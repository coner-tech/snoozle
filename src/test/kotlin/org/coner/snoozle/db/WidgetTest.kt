package org.coner.snoozle.db

import com.gregwoodfill.assert.shouldEqualJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*
import kotlin.reflect.KProperty1

class WidgetTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()

    @Before
    fun before() {
        SampleDb.copyTo(folder)
    }

    @Test
    fun itShouldGetEntityById() {
        val database = Database(folder.root, Widget::class)
        val expected = SampleDb.Widgets.One

        val actual = database.get(Widget::id to SampleDb.Widgets.One.id)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun itShouldPutEntityById() {
        val database = Database(folder.root, Widget::class)
        val entity = Widget(
                id = UUID.randomUUID(),
                name = "Widget Three"
        )

        database.put(entity)

        val record = SampleDb.Widgets.tempFile(folder, entity)
        assertThat(record).exists()
        val actual = record.readText()
        actual.shouldEqualJson("""{"id":"${entity.id}","name":"Widget Three"}""")
    }

    @Test
    fun itShouldRemoveEntity() {
        val database = Database(folder.root, Widget::class)
        val entity = SampleDb.Widgets.One
        val file = SampleDb.Widgets.tempFile(folder, entity)
        assertThat(file).exists() // sanity check

        database.remove(entity)

        assertThat(file).doesNotExist()
    }

    @Test
    fun itShouldList() {
        val database = Database(folder.root, Widget::class)
        val expected = listOf(SampleDb.Widgets.One, SampleDb.Widgets.Two)

        val actual: List<Widget> = database.list()

        assertThat(actual).isEqualTo(expected)
    }

}