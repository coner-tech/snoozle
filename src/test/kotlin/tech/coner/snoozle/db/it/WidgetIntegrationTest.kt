package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.exists
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
import assertk.assertions.isNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import tech.coner.snoozle.db.closeAndAssertSuccess
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.sample.Widget
import tech.coner.snoozle.db.sample.WidgetResource
import tech.coner.snoozle.db.sample.watchAll
import tech.coner.snoozle.db.sample.watchSpecific
import tech.coner.snoozle.db.session.data.DataSession
import tech.coner.snoozle.db.watch.EntityWatchEngine
import tech.coner.snoozle.db.watch.Event
import tech.coner.snoozle.db.watch.TestFileWatchEngine
import tech.coner.snoozle.db.watch.isInstanceOfDeleted
import tech.coner.snoozle.db.watch.isInstanceOfExists
import tech.coner.snoozle.db.watch.origin
import tech.coner.snoozle.db.watch.recordContent
import tech.coner.snoozle.db.watch.recordId
import tech.coner.snoozle.db.watch.scopes
import tech.coner.snoozle.db.watch.watchStore
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText
import kotlin.io.path.writeText

class WidgetIntegrationTest {

    @TempDir lateinit var root: Path

    private val defaultTimeoutMillis: Long = 1000L

    @Nested
    inner class CRUD {

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
    }

    @Test
    fun `It should stream Widgets`() = testWidgets {
        val widgets = widgets.stream()
                .toList()
                .sortedBy { it.name }

        assertThat(widgets).all {
            hasSize(2)
            containsAll(
                SampleDatabaseFixture.Widgets.One,
                SampleDatabaseFixture.Widgets.Two
            )
        }
    }

    @Nested
    inner class WatchApi {

        @Test
        fun `It should watch for any widget created in new directory`() = testWidgets(populate = false) {
            val widget = Widget(name = "Random Widget", widget = true)
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchAll())

            launch {
                widgets.create(widget)
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first { it.origin == Event.Origin.NEW_DIRECTORY_SCAN }
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
            assertThat(widgetsDirectory, "sanity check").exists()
            val widget = Widget(name = "Any widget", widget = true)
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchAll())

            launch { widgets.create(widget) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(Widget.Key(widget.id))
                    recordContent().isEqualTo(widget)
                    origin().isEqualTo(Event.Origin.RESOURCE_CREATED)
                }
        }

        @Test
        fun `It should watch for specific widget created`() = testWidgets {
            val widget = Widget(
                name = "Specific Widget Created",
                widget = true
            )
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchSpecific(widget.id))

            launch { widgets.create(widget) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(Widget.Key(widget.id))
                    recordContent().isEqualTo(widget)
                    origin().isIn(Event.Origin.RESOURCE_CREATED, Event.Origin.WATCH)
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
                    origin().isIn(Event.Origin.RESOURCE_CREATED, Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should watch for any widget modified`() = testWidgets(populate = false) {
            val original = Widget(name = "original", widget = true)
            widgets.create(original)
            val modified = original.copy(name = "modified")
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchAll())

            launch { widgets.update(modified) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(Widget.Key(original.id))
                    recordContent().isEqualTo(modified)
                    origin().isIn(Event.Origin.RESOURCE_UPDATED, Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should watch for any widget deleted`() = testWidgets(populate = false) {
            val original = Widget(name = "Widget to delete", widget = true)
            widgets.create(original)

            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchAll())

            launch { widgets.delete(original) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfDeleted()
                .all {
                    recordId().isEqualTo(Widget.Key(original.id))
                    origin().isIn(Event.Origin.RESOURCE_DELETED, Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should watch for widget with one remaining watch`() = testWidgets {
            val widget = Widget(
                name = "Create widget with one remaining watch",
                widget = true
            )
            val token = widgets.watchEngine.createToken()
            val watch1 = widgets.watchEngine.watchSpecific(widget.id)
            val watch2 = widgets.watchEngine.watchSpecific(widget.id)
            token.register(watch1)
            token.register(watch2)
            token.unregister(watch1)

            launch { widgets.create(widget) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(Widget.Key(widget.id))
                    recordContent().isEqualTo(widget)
                    origin().isIn(Event.Origin.RESOURCE_CREATED, Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should not watch for widget with no remaining watches`() = testWidgets {
            val widget = Widget(
                name = "Create widget with one remaining watch",
                widget = true
            )
            val token = widgets.watchEngine.createToken() as EntityWatchEngine.TokenImpl<Widget.Key, Widget>
            val watch1 = widgets.watchEngine.watchSpecific(widget.id)
            val watch2 = widgets.watchEngine.watchSpecific(widget.id)
            token.register(watch1)
            token.register(watch2)
            token.unregister(watch1)
            token.unregister(watch2)

            launch { widgets.create(widget) }
            assertThrows<TimeoutCancellationException> {
                withTimeout(defaultTimeoutMillis) {
                    token.events.firstOrNull()
                }
            }
        }

        @Test
        fun `It should not watch for widget when all watches unregistered`() = testWidgets {
            val widget = Widget(
                name = "Create widget with one remaining watch",
                widget = true
            )
            val token = widgets.watchEngine.createToken() as EntityWatchEngine.TokenImpl<Widget.Key, Widget>
            val watch1 = widgets.watchEngine.watchSpecific(widget.id)
            val watch2 = widgets.watchEngine.watchSpecific(widget.id)
            token.register(watch1)
            token.register(watch2)
            token.unregisterAll()

            launch { widgets.create(widget) }
            assertThrows<TimeoutCancellationException> {
                withTimeout(defaultTimeoutMillis) {
                    token.events.firstOrNull()
                }
            }
        }
    }

    @Nested
    inner class WatchSimulatedExternal {

        @Test
        fun `It should watch for any widget created in new directory`() = testWidgets(populate = false) {
            val widget = Widget(name = "Random Widget", widget = true)
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchAll())
            val widgetFile = root.resolve(SampleDatabaseFixture.Widgets.relativePath(widget).value)
            widgetFile.parent.createDirectories()
            val widgetAsJson = SampleDatabaseFixture.Widgets.asJson(widget)

            launch {
                widgetFile.writeText(widgetAsJson)
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first { it.origin == Event.Origin.NEW_DIRECTORY_SCAN }
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
            assertThat(widgetsDirectory, "sanity check").exists()
            val widget = Widget(name = "Any widget", widget = true)
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchAll())
            val widgetFile = root.resolve(SampleDatabaseFixture.Widgets.relativePath(widget).value)
            val widgetAsJson = SampleDatabaseFixture.Widgets.asJson(widget)

            launch {
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
                    origin().isEqualTo(Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should watch for specific widget created`() = testWidgets {
            val widget = Widget(
                name = "Specific Widget Created",
                widget = true
            )
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchSpecific(widget.id))
            val widgetFile = root.resolve(SampleDatabaseFixture.Widgets.relativePath(widget).value)
            val widgetAsJson = SampleDatabaseFixture.Widgets.asJson(widget)

            launch { widgetFile.writeText(widgetAsJson) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
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
        fun `It should not emit when different widget created than specific watched`() = testWidgets {
            val widgetOfInterest = Widget(
                name = "Specific Widget to Watch",
                widget = true
            )
            val widgetOfInterestFile = root.resolve(SampleDatabaseFixture.Widgets.relativePath(widgetOfInterest).value)
            val widgetOfInterestJson = SampleDatabaseFixture.Widgets.asJson(widgetOfInterest)
            val widgetNotOfInterest = Widget(
                name = "Widget of no concern with respect to Watch",
                widget = true
            )
            val widgetNotOfInterestFile = root.resolve(SampleDatabaseFixture.Widgets.relativePath(widgetOfInterest).value)
            val widgetNotOfInterestJson = SampleDatabaseFixture.Widgets.asJson(widgetNotOfInterest)
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchSpecific(widgetOfInterest.id))

            launch {
                widgetNotOfInterestFile.writeText(widgetNotOfInterestJson)
                widgetOfInterestFile.writeText(widgetOfInterestJson)
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(Widget.Key(widgetOfInterest.id))
                    recordContent().isEqualTo(widgetOfInterest)
                    origin().isEqualTo(Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should watch for any widget modified`() = testWidgets(populate = false) {
            val original = Widget(name = "original", widget = true)
            widgets.create(original)
            val modified = original.copy(name = "modified")
            val widgetFile = root.resolve(SampleDatabaseFixture.Widgets.relativePath(modified).value)
            val modifiedWidget = SampleDatabaseFixture.Widgets.asJson(modified)
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchAll())

            launch {
                widgetFile.writeText(modifiedWidget)
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
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
        fun `It should watch for any widget deleted`() = testWidgets(populate = false) {
            val widget = Widget(name = "Widget to delete", widget = true)
            widgets.create(widget)
            val widgetFile = root.resolve(SampleDatabaseFixture.Widgets.relativePath(widget).value)
            val token = widgets.watchEngine.createToken()
            token.register(widgets.watchEngine.watchAll())

            launch {
                widgetFile.deleteExisting()
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfDeleted()
                .all {
                    recordId().isEqualTo(Widget.Key(widget.id))
                    origin().isEqualTo(Event.Origin.WATCH)
                }
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
            context.session.closeAndAssertSuccess()
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