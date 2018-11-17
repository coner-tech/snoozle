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
import java.util.*
import kotlin.reflect.KProperty1

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

    @Test
    fun itShouldDelete() {
        val widget = SampleDb.Widgets.One
        val filePath = "/widgets/${widget.id}.json"
        every { path.findEntity(widget) }.returns(filePath)
        val record = SampleDb.Widgets.tempFile(folder, widget)
        assertThat(record).exists() // sanity check

        resource.delete(widget)

        verify { path.findEntity(widget) }
        assertThat(record).doesNotExist()
    }

    @Test
    fun itShouldList() {
        val widgets = listOf(
                SampleDb.Widgets.One,
                SampleDb.Widgets.Two
        )
        val ids = emptyArray<Pair<KProperty1<Widget, UUID>, UUID>>()
        val listingPath = "/widgets"
        every { path.findListing(*ids) }.returns(listingPath)
        every { objectMapper.readValue(
                match<File> { it.nameWithoutExtension == widgets[0].id.toString() },
                Widget::class.java
        ) }.returns(widgets[0])
        every { objectMapper.readValue(
                match<File> { it.nameWithoutExtension == widgets[1].id.toString() },
                Widget::class.java
        ) }.returns(widgets[1])

        val actual = resource.list(*ids)

        assertThat(actual).isEqualTo(widgets)
        val listedIds = widgets.map { it.id.toString() }
        verify(exactly = widgets.size) { objectMapper.readValue(
                match<File> { listedIds.contains(it.nameWithoutExtension) },
                Widget::class.java
        ) }
    }

}