package org.coner.snoozle

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
                id = "1f30d7b6-0296-489a-9615-55868aeef78a",
                name = "One"
        )

        val actual: Widget = database.getById(expected.id)

        assertThat(actual).isEqualTo(expected)
    }

}