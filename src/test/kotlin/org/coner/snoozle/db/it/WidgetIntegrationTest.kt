package org.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.snoozle.db.EntityEvent
import org.coner.snoozle.db.EntityResource
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Widget
import org.coner.snoozle.db.sample.getWidget
import org.coner.snoozle.util.readText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WidgetIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
    }

    @Test
    fun itShouldGetWidgets() {
        val widgets = arrayOf(SampleDb.Widgets.One, SampleDb.Widgets.Two)

        for (expected in widgets) {
            val actual = database.entity<Widget>().getWidget(expected.id)

            assertk.assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun itShouldPutWidget() {
        val widget = Widget(name = "Put Widget")

        database.entity<Widget>().put(widget)

        val expectedFile = SampleDb.Widgets.tempFile(root, widget)
        val expectedJson = SampleDb.Widgets.asJson(widget)
        Assertions.assertThat(expectedFile).exists()
        val actual = expectedFile.readText()
        JSONAssert.assertEquals(expectedJson, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldDeleteWidget() {
        val widgets = arrayOf(SampleDb.Widgets.One, SampleDb.Widgets.Two)

        for (widget in widgets) {
            val actualFile = SampleDb.Widgets.tempFile(root, widget)
            Assumptions.assumeThat(actualFile).exists()

            database.entity<Widget>().delete(widget)

            Assertions.assertThat(actualFile).doesNotExist()
        }
    }

    @Test
    fun itShouldListWidget() {
        val widgets = database.entity<Widget>().list()

        assertk.assertThat(widgets).all {
            hasSize(2)
            index(0).isEqualTo(SampleDb.Widgets.One)
            index(1).isEqualTo(SampleDb.Widgets.Two)
        }
    }

    @Test
    fun itShouldWatchListingForWidgets() {
        fun executeAndAssert(
                execute: EntityResource<Widget>.() -> Unit,
                awaitCount: Int = 1,
                assert: TestObserver<EntityEvent<Widget>>.() -> Unit
        ) {
            val testObserver = TestObserver<EntityEvent<Widget>>()
            database.entity<Widget>().watchListing()
                    .subscribeOn(Schedulers.io())
                    .subscribe(testObserver)

            // this is unfortunate, but i'm not seeing a more reliable way to wait just long enough for the
            // file watch to activate
            testObserver.await(50, TimeUnit.MILLISECONDS)
            execute(database.entity())

            testObserver.awaitCount(awaitCount)
            assert(testObserver)

            testObserver.dispose()
        }

        executeAndAssert(
                execute = { put(SampleDb.Widgets.One.copy(name = "changed")) },
                assert = {
                    assertValueCount(1)
                }
        )

        executeAndAssert(
                execute = { delete(SampleDb.Widgets.One) },
                assert = {
                    assertValueCount(1)
                }
        )

        executeAndAssert(
                execute = { put(SampleDb.Widgets.One) },
                assert = {
                    assertValueCount(1)
                    assertValueAt(0) {
                        it.watchEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE
                    }
                }
        )
    }

    private fun dumpValues(testObserver: TestObserver<EntityEvent<Widget>>) {
        testObserver.values().forEach {
            println("watchEvent=(kind=${it.watchEvent.kind()}, count=${it.watchEvent.count()})")
            println("entity=(${it.entity})")
            println("id=${it.id}")
            println()
        }
    }
}