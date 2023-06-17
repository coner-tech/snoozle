package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.exists
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
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
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import tech.coner.snoozle.db.closeAndAssertSuccess
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.sample.Subwidget
import tech.coner.snoozle.db.sample.SubwidgetResource
import tech.coner.snoozle.db.sample.Widget
import tech.coner.snoozle.db.sample.WidgetId
import tech.coner.snoozle.db.sample.WidgetResource
import tech.coner.snoozle.db.sample.subwidgets
import tech.coner.snoozle.db.sample.watchAllSubwidgets
import tech.coner.snoozle.db.sample.watchSubwidget
import tech.coner.snoozle.db.sample.watchSubwidgetsOf
import tech.coner.snoozle.db.sample.widgets
import tech.coner.snoozle.db.session.data.DataSession
import tech.coner.snoozle.db.watch.Event
import tech.coner.snoozle.db.watch.isInstanceOfDeleted
import tech.coner.snoozle.db.watch.isInstanceOfExists
import tech.coner.snoozle.db.watch.origin
import tech.coner.snoozle.db.watch.recordContent
import tech.coner.snoozle.db.watch.recordId
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.readText

class SubwidgetIntegrationTest {

    @TempDir
    lateinit var root: Path

    private val defaultTimeoutMillis: Long = 1000L

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

    @Nested
    inner class WatchApi {

        @Test
        fun `It should watch for any subwidget created in new directory`() = testSubwidgets(populate = false) {
            val widget = Widget(name = "Random Widget", widget = true)
            val widgetKey = widget.toKey()
            val subwidget = Subwidget(
                widgetId = widget.id,
                name = "Random Subwidget"
            )
            val key = subwidget.toKey()
            val token = subwidgets.createWatchToken()
            token.register(subwidgets.watchSubwidgetsOf(widgetKey))

            launch {
                subwidgets.create(subwidget)
            }
            val event = withTimeout(9999999 /*defaultTimeoutMillis*/) {
                token.events.first { it.origin == Event.Origin.NEW_DIRECTORY_SCAN }
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(key)
                    recordContent().isEqualTo(subwidget)
                    origin().isIn(Event.Origin.NEW_DIRECTORY_SCAN)
                }
        }

        @Test
        fun `It should watch for any subwidget created in existing directory`() = testSubwidgets {
            val widget = Widget(name = "Any widget", widget = true)
            widgets.create(widget)
            val subwidgetsDirectory = subwidgetsDirectory(widgetId = widget.id)
            assertThat(subwidgetsDirectory.createDirectories(), "sanity check").exists()
            val token = subwidgets.createWatchToken()
            token.register(subwidgets.watchAllSubwidgets())
            val subwidget = Subwidget(widgetId = widget.id, name = "subwidget to create in existing directory")

            launch { subwidgets.create(subwidget) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(subwidget.toKey())
                    recordContent().isEqualTo(subwidget)
                    origin().isEqualTo(Event.Origin.RESOURCE_CREATED)
                }
        }

        @Test
        fun `It should watch for specific subwidget created`() = testSubwidgets {
            val widget = Widget(
                name = "Specific Widget Created",
                widget = true
            )
            widgets.create(widget)
            val token = subwidgets.createWatchToken()
            val subwidget = Subwidget(
                name = "Specific subwidget",
                widgetId = widget.id
            )
            token.register(subwidgets.watchSubwidget(subwidget.toKey()))

            launch { subwidgets.create(subwidget) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(subwidget.toKey())
                    recordContent().isEqualTo(subwidget)
                    origin().isIn(Event.Origin.RESOURCE_CREATED, Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should not emit when different subwidget created than specific watched`() = testSubwidgets {
            val widget = Widget(
                name = "Some Widget",
                widget = true
            )
            widgets.create(widget)
            val subwidgetOfInterest = Subwidget(
                name = "Specific Subwidget to Watch",
                widgetId = widget.id
            )
            val subwidgetNotOfInterest = Subwidget(
                name = "Subwidget of no concern with respect to Watch",
                widgetId = widget.id
            )
            val token = subwidgets.createWatchToken()
            token.register(subwidgets.watchSubwidget(subwidgetOfInterest.toKey()))

            launch {
                subwidgets.create(subwidgetNotOfInterest)
                subwidgets.create(subwidgetOfInterest)
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(subwidgetOfInterest.toKey())
                    recordContent().isEqualTo(subwidgetOfInterest)
                    origin().isIn(Event.Origin.RESOURCE_CREATED, Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should watch for any subwidget modified`() = testSubwidgets(populate = false) {
            val widget = Widget(name = "Some Widget", widget = true)
            val original = Subwidget(name = "original", widgetId = widget.id)
            subwidgets.create(original)
            val modified = original.copy(name = "modified")
            val token = subwidgets.createWatchToken()
            token.register(subwidgets.watchAllSubwidgets())

            launch { subwidgets.update(modified) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(original.toKey())
                    recordContent().isEqualTo(modified)
                    origin().isIn(Event.Origin.RESOURCE_UPDATED, Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should watch for any subwidget deleted`() = testSubwidgets(populate = false) {
            val widget = Widget(name = "Some widget", widget = true)
            val original = Subwidget(name = "Widget to delete", widgetId = widget.id)
            subwidgets.create(original)

            val token = subwidgets.createWatchToken()
            token.register(subwidgets.watchAllSubwidgets())

            launch { subwidgets.delete(original) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfDeleted()
                .all {
                    recordId().isEqualTo(original.toKey())
                    origin().isIn(Event.Origin.RESOURCE_DELETED, Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should watch for subwidget with one remaining watch`() = testSubwidgets {
            val subwidget = Subwidget(
                name = "Create subwidget with one remaining watch",
                widgetId = UUID.randomUUID()
            )
            val token = subwidgets.createWatchToken()
            val watch1 = subwidgets.watchSubwidget(subwidget.toKey())
            val watch2 = subwidgets.watchSubwidget(subwidget.toKey())
            token.register(watch1)
            token.register(watch2)
            token.unregister(watch1)

            launch { subwidgets.create(subwidget) }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(subwidget.toKey())
                    recordContent().isEqualTo(subwidget)
                    origin().isIn(Event.Origin.RESOURCE_CREATED, Event.Origin.WATCH)
                }
        }

        @Test
        fun `It should not watch for subwidget with no remaining watches`() = testSubwidgets {
            val subwidget = Subwidget(
                name = "Create subwidget with no remaining watch",
                widgetId = UUID.randomUUID()
            )
            val subwidgetKey = subwidget.toKey()
            val token = subwidgets.createWatchToken()
            val watch1 = subwidgets.watchSubwidget(subwidgetKey)
            val watch2 = subwidgets.watchSubwidget(subwidgetKey)
            token.register(watch1)
            token.register(watch2)
            token.unregister(watch1)
            token.unregister(watch2)

            launch { subwidgets.create(subwidget) }
            assertThrows<TimeoutCancellationException> {
                withTimeout(defaultTimeoutMillis) {
                    token.events.firstOrNull()
                }
            }
        }

        @Test
        fun `It should not watch for subwidget when all watches across all tokens have unregistered`() = testSubwidgets {
            val subwidget = Subwidget(
                name = "Create subwidget should not be watched by any scopes",
                widgetId = UUID.randomUUID()
            )
            val subwidgetKey = subwidget.toKey()
            val token1 = subwidgets.createWatchToken()
            val token2 = subwidgets.createWatchToken()
            val watch1 = subwidgets.watchSubwidget(subwidgetKey)
            val watch2 = subwidgets.watchSubwidget(subwidgetKey)
            token1.register(watch1)
            token2.register(watch2)
            token1.unregister(watch1)
            token2.unregister(watch2)

            launch { subwidgets.create(subwidget) }
            assertThrows<TimeoutCancellationException> {
                withTimeout(defaultTimeoutMillis) {
                    token1.events.firstOrNull()
                }
            }
        }

        @Test
        fun `It should continue to watch for subwidget when same watch pattern registered on multiple scopes unregisters from one`() = testSubwidgets {
            val subwidget = Subwidget(
                name = "Watched by multiple scopes, one unregisters, one should remain watching",
                widgetId = UUID.randomUUID()
            )
            val subwidgetKey = subwidget.toKey()
            val token1 = subwidgets.createWatchToken()
            val token2 = subwidgets.createWatchToken()
            val watch = subwidgets.watchSubwidget(subwidgetKey)
            token1.register(watch)
            token2.register(watch)
            token1.unregister(watch)

            launch { subwidgets.create(subwidget) }
            val event = withTimeout(defaultTimeoutMillis) {
                token2.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(subwidgetKey)
                    recordContent().isEqualTo(subwidget)
                    origin().isIn(Event.Origin.RESOURCE_CREATED, Event.Origin.WATCH)
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
            widgets = session.widgets(),
            subwidgets = session.subwidgets(),
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
        val widgets: WidgetResource,
        val subwidgets: SubwidgetResource,
        val widgetsDirectory: Path
    ) : CoroutineScope {
        override val coroutineContext = Dispatchers.IO + Job()

        fun subwidgetsDirectory(widgetId: WidgetId): Path {
            return widgetsDirectory.resolve("$widgetId").resolve("subwidgets")
        }
    }
}