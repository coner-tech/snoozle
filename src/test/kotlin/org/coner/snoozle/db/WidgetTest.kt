package org.coner.snoozle.db

import com.gregwoodfill.assert.shouldEqualJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*

class WidgetTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()

    @Before
    fun before() {
        val uri = File(javaClass.getResource("/sample-db").toURI())
        uri.absoluteFile.copyRecursively(folder.root)
    }

    @Test
    fun itShouldGetEntityById() {
        val database = Database(folder.root, Widget::class)
        val expected = Widget(
                id = UUID.fromString("1f30d7b6-0296-489a-9615-55868aeef78a"),
                name = "One"
        )

        val actual: Widget = database.get(expected.id)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun itShouldPutEntityById() {
        val database = Database(folder.root, Widget::class)
        val entity = Widget(
                id = UUID.randomUUID(),
                name = "Three"
        )

        database.put(entity)

        val record = File(folder.root, "/widgets/${entity.id}.json")
        assertThat(record).exists()
        val actual = record.readText()
        actual.shouldEqualJson("""{"id":"${entity.id}","name":"Three"}""")
    }

    @Test
    fun itShouldRemoveById() {
        val database = Database(folder.root, Widget::class)
        val id = UUID.fromString("1f30d7b6-0296-489a-9615-55868aeef78a")
        val file = File(folder.root, "/widgets/$id.json")
        assertThat(file).exists() // sanity check
        val entity: Widget = database.get(id)

        database.remove(entity)

        assertThat(file).doesNotExist()
    }

}