package tech.coner.snoozle.db.watch

import assertk.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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

    private val defaultTimeoutMillis = 1000L

    @BeforeEach
    fun before() {
        database = SampleDatabaseFixture.factory(root, SampleDatabaseFixture.VERSION_HIGHEST)
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
            val token = widgets.watchEngine.createToken()
            token.registerAll()
            launch {
                withContext(Dispatchers.IO) {
                    widgets.create(SampleDatabaseFixture.Widgets.One) }
            }
            val event = withTimeout(defaultTimeoutMillis) {
                token.events.first()
            }

            assertThat(event)
                .isCreatedTypedInstance<Widget.Key, Widget>()
        }

        @Test
        fun `It should watch for any widget modified`() {
            TODO()
        }

        @Test
        fun `It should watch for any widget deleted`() {
            TODO()
        }
    }

    @Nested
    inner class Subwidgets {

    }
}