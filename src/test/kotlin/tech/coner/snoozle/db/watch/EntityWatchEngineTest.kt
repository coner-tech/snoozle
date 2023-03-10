package tech.coner.snoozle.db.watch

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
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
import tech.coner.snoozle.db.sample.*
import tech.coner.snoozle.db.session.data.DataSession
import tech.coner.snoozle.util.hasUuidPattern
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

class EntityWatchEngineTest : CoroutineScope {

    override val coroutineContext = Dispatchers.Default + Job()

    @TempDir lateinit var root: Path

    lateinit var database: SampleDatabase
    lateinit var dataSession: DataSession

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
        fun `watchAll should match any widget`() {
            val watch = widgets.watchEngine.watchAll()
            TODO()
        }

        @Test
        fun `watchSpecific single should match any single widget`() {
            val widget = SampleDatabaseFixture.Widgets.One
            val watch = widgets.watchEngine.watchSpecific(widget.id)
            TODO()
        }

        @Test
        fun `watchSpecific single should not match other widgets`() {
            TODO()
        }

        @Test
        fun `watchSpecific collection should match widgets from collection`() {
            TODO()
        }

        @Test
        fun `watchSpecific collection should not match widgets not from collection`() {
            TODO()
        }
    }


    @Nested
    inner class Subwidgets {

    }
}