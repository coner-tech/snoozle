package tech.coner.snoozle.db.watch

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import tech.coner.snoozle.db.sample.watchWidgets
import tech.coner.snoozle.db.sample.watchWidget
import tech.coner.snoozle.db.sample.watchAllWidgets
import tech.coner.snoozle.db.sample.widgets
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path

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

        private fun IntRange.randomWidgetRelativePathStrings() = map { Widget(name = "Widget $it", widget = true) }
            .map { it.toRelativePathString() }

        private fun Widget.toRelativePathString() = SampleDatabaseFixture.Widgets.relativePath(this).value.toString()

        @Test
        fun `watchAll filePattern should match any widget path`() {
            val randomWidgetPaths = (0..100).randomWidgetRelativePathStrings()
            val watch = widgets.watchAllWidgets()

            val actual = randomWidgetPaths
                .map { watch.filePattern.matcher(it).matches() }

            assertThat(actual, "match results for all widget paths").each { it.isTrue() }
        }

        @Test
        fun `watchSpecific single filePattern should match any single widget`() {
            val widget = SampleDatabaseFixture.Widgets.One
            val widgetPath = SampleDatabaseFixture.Widgets.relativePath(widget).value.toString()
            val watch = widgets.watchWidget(widget.id)

            val actual = watch.filePattern.matcher(widgetPath).matches()

            assertThat(actual, "match result for single widget path").isTrue()
        }

        @Test
        fun `watchSpecific single should not match other widgets`() {
            val randomOtherWidgetPaths = (0..100).randomWidgetRelativePathStrings()
            val widget = Widget(name = "Random Widget of Interest", widget = true)
            val watch = widgets.watchWidget(widget.id)

            val actual = randomOtherWidgetPaths
                .map { watch.filePattern.matcher(it).matches() }

            assertThat(actual, "match result for random other widget paths").each { it.isFalse() }
        }

        @Test
        fun `watchSpecific collection should match widgets from collection`() {
            val widgetsCollection = (0..100)
                .map { Widget(name = "Widget $it", widget = true) }
            val widgetsIds = widgetsCollection.map { it.id }
            val watch = widgets.watchWidgets(widgetsIds)
            val widgetsCollectionPaths = widgetsCollection
                .map { it.toRelativePathString() }

            val actual = widgetsCollectionPaths
                .map { watch.filePattern.matcher(it).matches() }

            assertThat(actual, "match result for widgets from collection").each { it.isTrue() }
        }

        @Test
        fun `watchSpecific collection should not match widgets not from collection`() {
            val widgetsOfInterest = (0..100)
                .map { Widget(name = "Widget $it", widget = true) }
                .map { it.id }
            val widgetsNotOfInterest = (0..100)
                .map { Widget(name = "Widget $it", widget = true) }
            val widgetsNotOfInterestPaths = widgetsNotOfInterest
                .map { it.toRelativePathString() }
            val watch = widgets.watchWidgets(widgetsOfInterest)

            val actual = widgetsNotOfInterestPaths
                .map { watch.filePattern.matcher(it).matches() }

            assertThat(actual, "match result for widgets not of interest").each { it.isFalse() }
        }
    }


    @Nested
    inner class Subwidgets {

    }
}