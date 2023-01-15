package tech.coner.snoozle.db.session.migration

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.exists
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.migration.MigrationPathMatcher
import tech.coner.snoozle.db.migration.MigrationTask
import tech.coner.snoozle.util.doesNotExist
import java.nio.file.Path
import java.nio.file.Paths
import tech.coner.snoozle.db.path.asAbsolute

class MigrationTaskTest {

    @Nested
    inner class MoveTest {

        @TempDir lateinit var vNullRoot: Path

        @BeforeEach
        fun before() {
            SampleDatabaseFixture.factory(root = vNullRoot, version = null)
        }

        @Test
        fun `It should move matching widget files`() {
            val widgetOne = SampleDatabaseFixture.Widgets.One
            val widgetTwo = SampleDatabaseFixture.Widgets.Two
            val from = listOf(
                MigrationPathMatcher.OnString("widget"),
                MigrationPathMatcher.OnDirectorySeparator,
                MigrationPathMatcher.OnUuid,
                MigrationPathMatcher.OnString(".json")
            )
            val to = listOf(
                MigrationPathMatcher.OnString("widgets"),
                MigrationPathMatcher.OnDirectorySeparator,
                MigrationPathMatcher.OnUuid,
                MigrationPathMatcher.OnString(".json")
            )
            val subject = MigrationTask.Move(from = from, to = to)
            val vNullWidgetOne = Paths.get("widget/${widgetOne.id}.json")
            val vNullWidgetTwo = Paths.get("widget/${widgetTwo.id}.json")

            subject.migrate(root = vNullRoot.asAbsolute())

            val movedWidgetOne = Paths.get("widgets/${widgetOne.id}.json")
            val movedWidgetTwo = Paths.get("widgets/${widgetTwo.id}.json")
            assertAll {
                assertThat(vNullRoot.resolve(vNullWidgetOne)).doesNotExist()
                assertThat(vNullRoot.resolve(vNullWidgetTwo)).doesNotExist()
                assertThat(vNullRoot.resolve(movedWidgetOne)).exists()
                assertThat(vNullRoot.resolve(movedWidgetTwo)).exists()
            }
        }

        @Test
        fun `It should move matching subwidget files`() {
            val widgetOneSubwidgetOne = SampleDatabaseFixture.Subwidgets.WidgetOneSubwidgetOne
            val widgetTwoSubwidgetOne = SampleDatabaseFixture.Subwidgets.WidgetTwoSubwidgetOne
            val from = listOf(
                MigrationPathMatcher.OnString("widget"),
                MigrationPathMatcher.OnDirectorySeparator,
                MigrationPathMatcher.OnUuid,
                MigrationPathMatcher.OnDirectorySeparator,
                MigrationPathMatcher.OnString("subwidget"),
                MigrationPathMatcher.OnDirectorySeparator,
                MigrationPathMatcher.OnUuid,
                MigrationPathMatcher.OnString(".json")
            )
            val to = listOf(
                MigrationPathMatcher.OnString("widgets"),
                MigrationPathMatcher.OnDirectorySeparator,
                MigrationPathMatcher.OnUuid,
                MigrationPathMatcher.OnDirectorySeparator,
                MigrationPathMatcher.OnString("subwidgets"),
                MigrationPathMatcher.OnDirectorySeparator,
                MigrationPathMatcher.OnUuid,
                MigrationPathMatcher.OnString(".json")
            )
            val subject = MigrationTask.Move(from = from, to = to)
            val vNullWidgetOneSubwidgetOne = Paths.get("widget/${widgetOneSubwidgetOne.widgetId}/subwidget/${widgetOneSubwidgetOne.id}.json")
            val vNullWidgetTwoSubwidgetOne = Paths.get("widget/${widgetTwoSubwidgetOne.widgetId}/subwidget/${widgetTwoSubwidgetOne.id}.json")

            subject.migrate(root = vNullRoot.asAbsolute())

            val movedWidgetOneSubwidgetOne = Paths.get("widgets/${widgetOneSubwidgetOne.widgetId}/subwidgets/${widgetOneSubwidgetOne.id}.json")
            val movedWidgetTwoSubwidgetOne = Paths.get("widgets/${widgetTwoSubwidgetOne.widgetId}/subwidgets/${widgetTwoSubwidgetOne.id}.json")
            assertAll {
                assertThat(vNullRoot.resolve(vNullWidgetOneSubwidgetOne)).doesNotExist()
                assertThat(vNullRoot.resolve(vNullWidgetTwoSubwidgetOne)).doesNotExist()
                assertThat(vNullRoot.resolve(movedWidgetOneSubwidgetOne)).exists()
                assertThat(vNullRoot.resolve(movedWidgetTwoSubwidgetOne)).exists()
            }
        }
    }
}