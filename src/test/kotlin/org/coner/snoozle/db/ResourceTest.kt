package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.Widget
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class ResourceTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    lateinit var resource: Resource<Widget>

    @MockK
    lateinit var objectMapper: ObjectMapper
    @MockK
    lateinit var path: Pathfinder<Widget>

    @Before
    fun before() {
        MockKAnnotations.init(this)
        resource = Resource(
                root = folder.root,
                kclass = Widget::class,
                objectMapper = objectMapper,
                path = path
        )
    }

    @Test
    fun itShouldGet() {
        val widget = SampleDb.Widgets.One
        val ids = arrayOf(Widget::id to widget.id)
        val fileSlot = slot<File>()
        val filePath = "/widgets/${widget.id}.json"
        every { path.findEntity(*ids) }.returns(filePath)
        every { objectMapper.readValue(capture(fileSlot), Widget::class.java) }.returns(widget)

        val actual = resource.get(*ids)

        verify { path.findEntity(*ids) }
        verify { objectMapper.readValue(fileSlot.captured, Widget::class.java) }
        assertThat(fileSlot.captured)
                .hasParent(File(folder.root, "/widgets"))
                .hasName("${widget.id}.json")
        assertThat(actual).isSameAs(widget)
    }

}