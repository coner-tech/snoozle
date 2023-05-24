package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import tech.coner.snoozle.db.closeAndAssertSuccess
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.sample.Subwidget
import tech.coner.snoozle.db.sample.SubwidgetResource
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path
import java.util.*
import kotlin.io.path.readText

class SubwidgetIntegrationTest {

    @TempDir
    lateinit var root: Path

    @Nested
    inner class CRUD {

        @Test
        fun `It should read a Subwidget`() = testSubwidgets {
            val widgetOneSubwidgetOne = SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne
            val key = Subwidget.Key(
                widgetId = widgetOneSubwidgetOne.widgetId,
                id = widgetOneSubwidgetOne.id
            )

            val actual = subwidgets.read(key)

            assertThat(actual).isEqualTo(widgetOneSubwidgetOne)
        }

        @Test
        fun `It should create a Subwidget`() = testSubwidgets {
            val subwidget = Subwidget(
                widgetId = SampleDatabaseFixture.Widgets.Two.id,
                name = "Widget Two Subwidget Two"
            )

            subwidgets.create(subwidget)

            val expectedFile = SampleDatabaseFixture.Subwidgets.tempFile(root, subwidget)
            Assertions.assertThat(expectedFile).exists()
            val expectedJson = SampleDatabaseFixture.Subwidgets.asJson(subwidget)
            val actualJson = expectedFile.readText()
            JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
        }

        @Test
        fun `It should update a Subwidget`() = testSubwidgets {
            val widgetOneSubwidgetOne = SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne
            val expectedFile = SampleDatabaseFixture.Subwidgets.tempFile(root, widgetOneSubwidgetOne)
            Assumptions.assumeThat(expectedFile).exists()
            val update = widgetOneSubwidgetOne.copy(
                name = "Updated"
            )
            val beforeJson = expectedFile.readText()
            val expectedJson = SampleDatabaseFixture.Subwidgets.asJson(update)

            subwidgets.update(update)

            val actualJson = expectedFile.readText()
            JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
            JSONAssert.assertNotEquals(beforeJson, actualJson, JSONCompareMode.LENIENT)
        }

        @Test
        fun `It should delete Subwidget`() = testSubwidgets {
            val widgetOneSubwidgetOne = SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne
            val actualFile = SampleDatabaseFixture.Subwidgets.tempFile(root, widgetOneSubwidgetOne)
            Assumptions.assumeThat(actualFile).exists()

            subwidgets.delete(widgetOneSubwidgetOne)

            Assertions.assertThat(actualFile).doesNotExist()
        }

        @Test
        fun `It should stream Subwidgets`() = testSubwidgets {
            val actual = subwidgets.stream()
                .toList()
                .sortedBy { it.name }

            assertThat(actual).all {
                hasSize(2)
                containsAll(
                    SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne,
                    SampleDatabaseFixture.Subwidgets.WidgetTwoSubwidgetOne
                )
            }
        }
    }

    private fun testSubwidgets(populate: Boolean = true, testFn: suspend TestContext.() -> Unit) {
        val session = SampleDatabaseFixture
            .factory(
                root = root,
                version = SampleDatabaseFixture.VERSION_HIGHEST,
                populate = populate
            )
            .apply {
                if (!populate) openAdministrativeSession().getOrThrow()
                    .use { it.initializeDatabase().getOrThrow() }
            }
            .openDataSession()
            .getOrThrow()
        val context = TestContext(
            session = session,
            subwidgets = session.entity(),
            widgetsDirectory = root.resolve("widgets")
        )
        try {
            runBlocking { testFn(context) }
        } finally {
            runBlocking { context.subwidgets.destroyAllWatchTokens() }
            context.session.closeAndAssertSuccess()
            context.cancel()
        }
    }

    private class TestContext(
        val session: DataSession,
        val subwidgets: SubwidgetResource,
        val widgetsDirectory: Path
    ) : CoroutineScope {
        override val coroutineContext = Dispatchers.IO + Job()

        fun subwidgetsDirectory(widgetId: UUID): Path {
            return widgetsDirectory.resolve("$widgetId").resolve("subwidgets")
        }
    }
}