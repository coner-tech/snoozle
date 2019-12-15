package org.coner.snoozle.db.it

import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.coner.snoozle.db.entity.Entity
import org.coner.snoozle.db.entity.EntityEvent
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Widget
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class WatchEntityIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase
    private lateinit var widgetObserver: TestObserver<EntityEvent<Widget>>

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
        widgetObserver = observe()
    }

    @AfterEach
    fun after() {
        widgetObserver.dispose()
    }

    @Test
    fun itShouldEmitWhenPutChangesEntity() {
        val changed = SampleDb.Widgets.One.copy(name = "changed")

        database.entity<Widget>().put(changed)

        widgetObserver.run {
            awaitCount(1)
            assertValueCount(1)
            assertValueAt(0) {
                it.state == EntityEvent.State.EXISTS
                        && it.entity == changed
            }
        }
    }

    @Test
    fun itShouldEmitWhenDeleteRemovesEntity() {
        database.entity<Widget>().delete(SampleDb.Widgets.One)

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
    fun itShouldEmitWhenPutCreatesEntity() {
        val created = Widget(name = "created")

        database.entity<Widget>().put(created)

        widgetObserver.run {
            awaitCount(1)
            assertValueCount(1)
            assertValueAt(0) {
                it.state == EntityEvent.State.EXISTS
                        && it.entity == created
            }
        }
    }

    @Test
    fun itShouldNotEmitWhenJunkDataWritten() {
        val widget = Widget(
                name = "itShouldNotEmitWhenJunkDataWritten"
        )
        val widgetAsJson = SampleDb.Widgets.asJson(widget)
        val halfWidgetAsJson = widgetAsJson.substring(0..(widgetAsJson.length.div(2)))
        val widgetPath = root.resolve(Paths.get("widgets", "${widget.id}.json"))

        // sanity check the observer before testing
        database.entity<Widget>().put(widget)
        widgetObserver.awaitCount(1)
        widgetObserver.assertValueAt(0) {
            it.state == EntityEvent.State.EXISTS
                    && it.entity == widget
        }

        // actually write some junk data
        widgetPath.toFile().writeText(halfWidgetAsJson)

        // sanity check the observer afterwards to ensure it has had its chance to emit or blow up somehow
        database.entity<Widget>().delete(widget)
        val revisedWidget = widget.copy(name = "revisedWidget")
        widgetObserver.awaitCount(2)
        widgetObserver.assertValueAt(1) {
            it.state == EntityEvent.State.DELETED
                    && it.entity == null
        }
        database.entity<Widget>().put(revisedWidget)
        widgetObserver.awaitCount(3)
        widgetObserver.assertValueAt(2) {
            it.state == EntityEvent.State.EXISTS
                    && it.entity == revisedWidget
        }
        widgetObserver.assertValueCount(3) // expect only sanity check values
        widgetObserver.assertNoErrors()
    }

    private inline fun <reified E : Entity> observe(): TestObserver<EntityEvent<E>> {
        val testObserver = TestObserver<EntityEvent<E>>()
        database.entity<E>().watchListing()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        // this is unfortunate, but i'm not seeing a more reliable way to wait just long enough for the
        // file watch to activate
        testObserver.await(50, TimeUnit.MILLISECONDS)
        return testObserver
    }
}