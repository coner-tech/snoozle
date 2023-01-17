package tech.coner.snoozle.db.watch

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

class EntityWatchEngineTest : CoroutineScope {

    override val coroutineContext = Dispatchers.IO + Job()

    @TempDir lateinit var root: Path

    lateinit var database: SampleDatabase
    lateinit var dataSession: DataSession

    private val defaultTimeoutMillis = 5000L

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

        @BeforeEach
        fun before() {
            widgets = dataSession.widgets()
        }

        @Test
        fun `It should watch for any widget created`(): Unit = runBlocking {
            val created = SampleDatabaseFixture.Widgets.One
            val token = widgets.watchEngine.createToken()
            token.registerAll()

            launch {
                widgets.create(created)
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(Widget.Key(created.id))
                    recordContent().isEqualTo(created)
                }
        }

        @Test
        fun `It should watch for any widget modified`() = runBlocking {
            val original = SampleDatabaseFixture.Widgets.One
            widgets.create(original)
            val modified = original.copy(name = "modified")

            val token = widgets.watchEngine.createToken()
            token.registerAll()

            launch(Dispatchers.IO) {
                widgets.update(modified)
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfExists()
                .all {
                    recordId().isEqualTo(Widget.Key(original.id))
                    recordContent().isEqualTo(modified)
                }
        }

        @Test
        fun `It should watch for any widget deleted`() = runBlocking {
            val original = SampleDatabaseFixture.Widgets.One
            widgets.create(original)

            val token = widgets.watchEngine.createToken()
            token.registerAll()

            launch {
                widgets.delete(original)
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isInstanceOfDeleted()
                .recordId().isEqualTo(Widget.Key(original.id))
        }
    }

    @Nested
    inner class Subwidgets {

    }
}