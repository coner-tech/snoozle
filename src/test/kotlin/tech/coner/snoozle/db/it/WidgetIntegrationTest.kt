package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.sample.Widget
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path
import kotlin.io.path.readText

class WidgetIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var session: DataSession
    private lateinit var resource: EntityResource<Widget.Key, Widget>

    @BeforeEach
    fun before() {
        session = SampleDatabaseFixture
            .factory(
                root = root,
                version = SampleDatabaseFixture.VERSION_HIGHEST
            )
            .openDataSession()
            .getOrThrow()
        resource = session.entity()
    }

    @AfterEach
    fun after() {
        session.close()
    }

    @Test
    fun `It should get a Widget`() {
        val widget = SampleDatabaseFixture.Widgets.One
        val key = Widget.Key(id = widget.id)

        val actual = resource.read(key)

        assertk.assertThat(actual).isEqualTo(widget)
    }

    @Test
    fun `It should create a Widget`() {
        val widget = Widget(name = "Create a Widget", widget = true)

        resource.create(widget)

        val expectedFile = SampleDatabaseFixture.Widgets.tempFile(root, widget)
        val expectedJson = SampleDatabaseFixture.Widgets.asJson(widget)
        Assertions.assertThat(expectedFile).exists()
        val actual = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should delete a Widget`() {
        val widget = SampleDatabaseFixture.Widgets.One
        val actualFile = SampleDatabaseFixture.Widgets.tempFile(root, widget)
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
            index(0).isEqualTo(SampleDatabaseFixture.Widgets.One)
            index(1).isEqualTo(SampleDatabaseFixture.Widgets.Two)
        }
    }
}