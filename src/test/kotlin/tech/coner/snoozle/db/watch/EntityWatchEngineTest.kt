package tech.coner.snoozle.db.watch

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.path.asAbsolute
import tech.coner.snoozle.db.sample.SampleDatabase
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.sample.Widget
import tech.coner.snoozle.db.sample.WidgetResource
import tech.coner.snoozle.db.sample.widgets
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

class EntityWatchEngineTest : CoroutineScope {

    override val coroutineContext = Dispatchers.Default + Job()

    @TempDir lateinit var root: Path

    lateinit var database: SampleDatabase
    lateinit var dataSession: DataSession

    private val defaultTimeoutMillis: Long = 5000L

    @BeforeEach
    fun before() {
        database = SampleDatabase(root.asAbsolute())
        database.openAdministrativeSession().getOrThrow()
            .use { it.initializeDatabase() }
        dataSession = database.openDataSession().getOrThrow()
    }

    @AfterEach
    fun after() {
        dataSession.close()
    }

    @Nested
    inner class Widgets {

        lateinit var widgets: WidgetResource
        lateinit var widgetsDirectory: Path

        @BeforeEach
        fun before() {
            widgets = dataSession.widgets()
            widgetsDirectory = root.resolve("widgets")
        }

        @AfterEach
        fun after () = runBlocking {
            widgets.watchEngine.destroyAllTokens()
        }

        @Test
        fun `It should watch for any widget created in new directory`(): Unit = runBlocking {
            val widget = SampleDatabaseFixture.Widgets.One
            val widgetAsJson = SampleDatabaseFixture.Widgets.asJson(widget)
            val widgetFile = widgetsDirectory.resolve("${widget.id}.json")
            val token = widgets.watchEngine.createToken()
            token.registerAll()

            launch {
                widgetsDirectory.createDirectory()
                widgetFile.writeText(widgetAsJson)
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events
                    .filter { it.origin == Event.Origin.NEW_DIRECTORY_SCAN }
                    .first()
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
        fun `It should watch for any widget created in existing directory`() = runBlocking {
            widgetsDirectory.createDirectory()
            val widget = SampleDatabaseFixture.Widgets.One
            val widgetAsJson = SampleDatabaseFixture.Widgets.asJson(widget)
            val widgetFile = widgetsDirectory.resolve("${widget.id}.json")
            val token = widgets.watchEngine.createToken()
            token.registerAll()

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
        fun `It should watch for any widget modified`() = runBlocking {
            val original = SampleDatabaseFixture.Widgets.One
            widgets.create(original)
            val widgetFile = widgetsDirectory.resolve("${original.id}.json")
            val modified = original.copy(name = "modified")
            val modifiedAsJson = SampleDatabaseFixture.Widgets.asJson(modified)
            val token = widgets.watchEngine.createToken()
            token.registerAll()

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
        fun `It should watch for any widget deleted`() = runBlocking {
            val original = SampleDatabaseFixture.Widgets.One
            widgets.create(original)

            val token = widgets.watchEngine.createToken()
            token.registerAll()

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
    }

    @Nested
    inner class Subwidgets {

    }
}