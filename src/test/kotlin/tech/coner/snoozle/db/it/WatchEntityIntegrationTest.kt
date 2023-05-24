package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.reactivex.Observer
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.closeAndAssertSuccess
import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.entity.EntityEvent
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.sample.Widget
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class WatchEntityIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var session: DataSession
    private lateinit var resource: EntityResource<Widget.Key, Widget>
    private lateinit var widgetObserver: TestObserver<EntityEvent<Widget.Key, Widget>>

    @BeforeEach
    fun before() {
        session = SampleDatabaseFixture
            .factory(
                root = root,
                version = SampleDatabaseFixture.VERSION_HIGHEST
            )
            .openDataSession()
            .getOrThrow()
        resource = session.entity()
        widgetObserver = observe()
    }

    @AfterEach
    fun after() {
        widgetObserver.dispose()
        session.closeAndAssertSuccess()
    }

    @Test
    fun `It should emit when Widget updated`() {
        val changed = SampleDatabaseFixture.Widgets.One.copy(name = "changed")

        resource.update(changed)

        widgetObserver.awaitCount(1)
        val actual: List<EntityEvent<Widget.Key, Widget>> = widgetObserver.values()

        assertThat(actual).all {
            hasSize(1)
            index(0).all {
                prop("state") { it.state }.isEqualTo(EntityEvent.State.EXISTS)
                prop("entity") { it.entity }.isEqualTo(changed)
            }
        }
    }

    @Test
    fun `It should emit when delete removes Widget`() {
        resource.delete(SampleDatabaseFixture.Widgets.One)

        widgetObserver.run {
            awaitCount(1)
            assertValueCount(1)
            assertValueAt(0) {
                it.state == EntityEvent.State.DELETED
                        && it.entity == null
            }
        }
    }

    @Test
    fun `It should emit when Widget created`() {
        val create = Widget(name = "created", widget = true)

        resource.create(create)

        widgetObserver.run {
            awaitCount(1)
            assertValueCount(1)
            assertValueAt(0) {
                it.state == EntityEvent.State.EXISTS
                        && it.entity == create
            }
        }
    }

    @Test
    fun `It should not emit when junk data written`() {
        val widget = Widget(
            name = "It should not emit when junk data written",
            widget = true
        )
        val widgetAsJson = SampleDatabaseFixture.Widgets.asJson(widget)
        // cut the json in half, simulates a non-atomic update
        val halfWidgetAsJson = widgetAsJson.substring(0..(widgetAsJson.length.div(2)))
        val widgetPath = root.resolve(Paths.get("widgets", "${widget.id}.json"))

        // sanity check the observer before testing
        resource.create(widget)
        widgetObserver.awaitCount(1)
        widgetObserver.assertValueAt(0) {
            it.state == EntityEvent.State.EXISTS
                    && it.entity == widget
        }

        // write junk data
        widgetPath.toFile().writeText(halfWidgetAsJson)

        // sanity check the observer afterwards to ensure it has had its chance to emit or blow up somehow
        resource.delete(widget)
        val revisedWidget = widget.copy(name = "Revised")
        widgetObserver.awaitCount(2)
        widgetObserver.assertValueAt(1) {
            it.state == EntityEvent.State.DELETED
                    && it.entity == null
        }
        resource.create(revisedWidget)
        widgetObserver.awaitCount(3)
        widgetObserver.assertValueAt(2) {
            it.state == EntityEvent.State.EXISTS
                    && it.entity == revisedWidget
        }
        widgetObserver.assertValueCount(3) // expect only sanity check values
        widgetObserver.assertNoErrors()
    }

    private inline fun <reified K : tech.coner.snoozle.db.Key, reified E : Entity<K>> observe(): TestObserver<EntityEvent<K, E>> {
        val testObserver: TestObserver<EntityEvent<K, E>> = TestObserver()
        resource.watchRxJava()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver as Observer<in EntityEvent<Widget.Key, Widget>>)

        // this is unfortunate, but i'm not seeing a more reliable way to wait just long enough for the
        // file watch to activate
        testObserver.await(50, TimeUnit.MILLISECONDS)
        return testObserver
    }
}