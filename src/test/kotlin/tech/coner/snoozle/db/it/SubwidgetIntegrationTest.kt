package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
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
import tech.coner.snoozle.db.sample.Subwidget
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path
import kotlin.io.path.readText

class SubwidgetIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var session: DataSession
    private lateinit var resource: EntityResource<Subwidget.Key, Subwidget>

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
    fun `It should read a Subwidget`() {
        val widgetOneSubwidgetOne = SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne
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
                widgetId = SampleDatabaseFixture.Widgets.Two.id,
                name = "Widget Two Subwidget Two"
        )

        resource.create(subwidget)

        val expectedFile = SampleDatabaseFixture.Subwidgets.tempFile(root, subwidget)
        Assertions.assertThat(expectedFile).exists()
        val expectedJson = SampleDatabaseFixture.Subwidgets.asJson(subwidget)
        val actualJson = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should update a Subwidget`() {
        val widgetOneSubwidgetOne = SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne
        val expectedFile = SampleDatabaseFixture.Subwidgets.tempFile(root, widgetOneSubwidgetOne)
        Assumptions.assumeThat(expectedFile).exists()
        val update = widgetOneSubwidgetOne.copy(
                name = "Updated"
        )
        val beforeJson = expectedFile.readText()
        val expectedJson = SampleDatabaseFixture.Subwidgets.asJson(update)

        resource.update(update)

        val actualJson = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
        JSONAssert.assertNotEquals(beforeJson, actualJson, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should delete Subwidget`() {
        val widgetOneSubwidgetOne = SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne
        val actualFile = SampleDatabaseFixture.Subwidgets.tempFile(root, widgetOneSubwidgetOne)
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
            index(0).isEqualTo(SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne)
            index(1).isEqualTo(SampleDatabaseFixture.Subwidgets.WidgetTwoSubwidgetOne)

        }
    }

}