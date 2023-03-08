package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import tech.coner.snoozle.db.sample.*
import tech.coner.snoozle.db.session.data.DataSession
import tech.coner.snoozle.db.watch.*
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText

class WidgetIntegrationTest {

    @TempDir lateinit var root: Path

    private val defaultTimeoutMillis: Long = 5000L

    @Test
    fun `It should get a Widget`() = testWidgets {
        val widget = SampleDatabaseFixture.Widgets.One
        val key = Widget.Key(id = widget.id)

        val actual = widgets.read(key)

        assertThat(actual).isEqualTo(widget)
    }

    @Test
    fun `It should create a Widget`() = testWidgets {
        val widget = Widget(name = "Create a Widget", widget = true)

        widgets.create(widget)

        val expectedFile = SampleDatabaseFixture.Widgets.tempFile(root, widget)
        val expectedJson = SampleDatabaseFixture.Widgets.asJson(widget)
        Assertions.assertThat(expectedFile).exists()
        val actual = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should delete a Widget`() = testWidgets {
        val widget = SampleDatabaseFixture.Widgets.One
        val actualFile = SampleDatabaseFixture.Widgets.tempFile(root, widget)
        Assumptions.assumeThat(actualFile).exists()

        widgets.delete(widget)

        Assertions.assertThat(actualFile).doesNotExist()
    }

    @Test
    fun `It should stream Widgets`() = testWidgets {
        val widgets = widgets.stream()
                .toList()
                .sortedBy { it.name }

        assertThat(widgets).all {
            hasSize(2)
            index(0).isEqualTo(SampleDatabaseFixture.Widgets.One)
            index(1).isEqualTo(SampleDatabaseFixture.Widgets.Two)
        }
    }


    @Test
    fun `It should watch for any widget created in new directory`(): Unit = testWidgets {
        val widget = Widget(name = "Random Widget", widget = true)
        val token = widgets.watchEngine.createToken()
        token.register(widgets.watchEngine.watchAll())

        launch {
            widgetsDirectory.createDirectories()
            widgets.create(widget)
        }
        val event = withTimeout(defaultTimeoutMillis) {
            token.events.first()
        }

        assertThat(event)
            .isInstanceOfExists()
            .all {
                recordId().isEqualTo(Widget.Key(widget.id))
                recordContent().isEqualTo(widget)
                origin().isEqualTo(Event.Origin.NEW_DIRECTORY_SCAN)
            }
    }

    @Test
    fun `It should watch for any widget created in existing directory`() = testWidgets {
        widgetsDirectory.createDirectory()
        val widget = SampleDatabaseFixture.Widgets.One
        val widgetAsJson = SampleDatabaseFixture.Widgets.asJson(widget)
        val widgetFile = widgetsDirectory.resolve("${widget.id}.json")
        val token = widgets.watchEngine.createToken()
        token.register(widgets.watchEngine.watchAll())

        launch { widgetFile.writeText(widgetAsJson) }
        val event = withTimeout(defaultTimeoutMillis) {
            token.events
                .filter { it.origin == Event.Origin.WATCH }
                .first()
        }

        assertThat(event)
            .isInstanceOfExists()
            .all {
                recordId().isEqualTo(Widget.Key(widget.id))
                recordContent().isEqualTo(widget)
                origin().isEqualTo(Event.Origin.WATCH)
            }
    }

    @Test
    fun `It should watch for specific widget created`(): Unit = testWidgets {
        val widget = Widget(
            name = "Specific Widget Created",
            widget = true
        )
        val widgetAsJson = SampleDatabaseFixture.Widgets.asJson(widget)
        val widgetFile = widgetsDirectory.resolve("${widget.id}.json")
        val token = widgets.watchEngine.createToken()
        token.register(widgets.watchEngine.watchSpecific(widget.id))

        launch {
            widgetsDirectory.createDirectories()
            widgetFile.writeText(widgetAsJson)
        }
        val event = withTimeout(defaultTimeoutMillis) {
            token.events.first()
        }

        assertThat(event)
            .isInstanceOfExists()
            .all {
                recordId().isEqualTo(Widget.Key(widget.id))
                recordContent().isEqualTo(widget)
            }
    }

    @Test
    fun `It should not emit when different widget created than specific watched`() = testWidgets {
        val widgetOfInterest = Widget(
            name = "Specific Widget to Watch",
            widget = true
        )
        val widgetNotOfInterest = Widget(
            name = "Widget of no concern with respect to Watch",
            widget = true
        )
        val token = widgets.watchEngine.createToken()
        token.register(widgets.watchEngine.watchSpecific(widgetOfInterest.id))

        launch {
            widgets.create(widgetNotOfInterest)
            widgets.create(widgetOfInterest)
        }
        val event = withTimeout(defaultTimeoutMillis) {
            token.events.first()
        }

        assertThat(event)
            .isInstanceOfExists()
            .all {
                recordId().isEqualTo(Widget.Key(widgetOfInterest.id))
                recordContent().isEqualTo(widgetOfInterest)
            }
    }

    @Test
    fun `It should watch for any widget modified`() = testWidgets(populate = false) {
        val original = SampleDatabaseFixture.Widgets.One
        widgets.create(original)
        val widgetFile = widgetsDirectory.resolve("${original.id}.json")
        val modified = original.copy(name = "modified")
        val modifiedAsJson = SampleDatabaseFixture.Widgets.asJson(modified)
        val token = widgets.watchEngine.createToken()
        token.register(widgets.watchEngine.watchAll())

        launch { widgetFile.writeText(modifiedAsJson) }
        val event = withTimeout(defaultTimeoutMillis) {
            token.events
                .filter { it.origin == Event.Origin.WATCH }
                .first()
        }

        assertThat(event)
            .isInstanceOfExists()
            .all {
                recordId().isEqualTo(Widget.Key(original.id))
                recordContent().isEqualTo(modified)
                origin().isEqualTo(Event.Origin.WATCH)
            }
    }

    @Test
    fun `It should watch for any widget deleted`() = testWidgets {
        val original = SampleDatabaseFixture.Widgets.One
        widgets.create(original)

        val token = widgets.watchEngine.createToken()
        token.register(widgets.watchEngine.watchAll())

        launch { widgets.delete(original) }
        val event = withTimeout(defaultTimeoutMillis) {
            token.events
                .filter { it.origin == Event.Origin.WATCH }
                .first() }

        assertThat(event)
            .isInstanceOfDeleted()
            .all {
                recordId().isEqualTo(Widget.Key(original.id))
                origin().isEqualTo(Event.Origin.WATCH)
            }
    }

    private fun testWidgets(populate: Boolean = true, testFn: suspend TestContext.() -> Unit) {
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
            widgets = session.entity(),
            widgetsDirectory = root.resolve("widgets")
        )
        try {
            runBlocking { testFn(context) }
        } finally {
            runBlocking { context.widgets.watchEngine.destroyAllTokens() }
            context.session.close()
            context.cancel()
        }
    }

    private class TestContext(
        val session: DataSession,
        val widgets: WidgetResource,
        val widgetsDirectory: Path
    ) : CoroutineScope {
        override val coroutineContext = Dispatchers.IO + Job()
    }
}