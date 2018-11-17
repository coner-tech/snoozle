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
import java.io.OutputStream

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
        SampleDb.copyTo(folder)
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
        val filePath = "/widgets/${widget.id}.json"
        every { path.findEntity(*ids) }.returns(filePath)
        every { objectMapper.readValue(any<File>(), Widget::class.java) }.returns(widget)

        val actual = resource.get(*ids)

        verify { path.findEntity(*ids) }
        verify { objectMapper.readValue(
                    match<File> { it.absolutePath.endsWith(filePath) },
                    Widget::class.java
        )}
        assertThat(actual).isSameAs(widget)
    }

    @Test
    fun itShouldPut() {
        val widget = SampleDb.Widgets.One
        val filePath = "/widgets/${widget.id}.json"
        every { path.findEntity(widget) }.returns(filePath)
        every { objectMapper.writeValue(any<File>(), any()) } just Runs

        resource.put(widget)

        verify { objectMapper.writeValue(
                match<File> { it.absolutePath.endsWith(filePath) },
                widget
        ) }
    }

}