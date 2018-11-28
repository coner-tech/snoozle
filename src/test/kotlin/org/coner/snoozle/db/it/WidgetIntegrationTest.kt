package org.coner.snoozle.db.it

import assertk.assert
import assertk.assertions.*
import com.gregwoodfill.assert.shouldEqualJson
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.coner.snoozle.db.Database
import org.coner.snoozle.db.jvm.EntityEvent
import org.coner.snoozle.db.jvm.watchListing
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Widget
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.util.*
import java.util.concurrent.TimeUnit

class WidgetIntegrationTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()

    @Before
    fun before() {
        SampleDb.copyTo(folder)
    }

    @Test
    fun itShouldGetEntityById() {
        val database = Database(folder.root, Widget::class)
        val expected = SampleDb.Widgets.One

        val actual = database.get(Widget::id to SampleDb.Widgets.One.id)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun itShouldPutEntityById() {
        val database = Database(folder.root, Widget::class)
        val entity = Widget(
                id = UUID.randomUUID(),
                name = "Widget Three"
        )

        database.put(entity)

        val record = SampleDb.Widgets.tempFile(folder, entity)
        assertThat(record).exists()
        val actual = record.readText()
        actual.shouldEqualJson("""{"id":"${entity.id}","name":"Widget Three"}""")
    }

    @Test
    fun itShouldRemoveEntity() {
        val database = Database(folder.root, Widget::class)
        val entity = SampleDb.Widgets.One
        val file = SampleDb.Widgets.tempFile(folder, entity)
        assertThat(file).exists() // sanity check

        database.remove(entity)

        assertThat(file).doesNotExist()
    }

    @Test
    fun itShouldList() {
        val database = Database(folder.root, Widget::class)
        val expected = listOf(SampleDb.Widgets.One, SampleDb.Widgets.Two)

        val actual: List<Widget> = database.list()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun itShouldWatchEntityAdded() {
        val database = Database(folder.root, Widget::class)
        val widget = Widget(
                UUID.randomUUID(),
                "Added Widget"
        )

        val actual = database.watchListing<Widget>()
                .doOnSubscribe {
                    Single.just(widget)
                            .observeOn(Schedulers.io())
                            .delay(20, TimeUnit.MILLISECONDS)
                            .subscribe { widget -> database.put(widget) }
                }
                .filter { it.entity != null }
                .blockingFirst()

        assert(actual).isNotNull {
            it.prop(EntityEvent<Widget>::watchEvent)
                    .prop(WatchEvent<*>::kind)
                    .isIn(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
            it.prop(EntityEvent<Widget>::entity).isNotNull {
                it.isEqualTo(widget)
            }
        }
    }

    @Test
    fun itShouldWatchEntityModified() {
        val database = Database(folder.root, Widget::class)
        val widget = SampleDb.Widgets.One
        val changed = widget.copy(name = "Changed")

        val actual = database.watchListing<Widget>()
                .doOnSubscribe { Single.just(widget)
                        .observeOn(Schedulers.io())
                        .delay(20, TimeUnit.MILLISECONDS)
                        .subscribe { widget -> database.put(changed) }
                }
                .filter { it.watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY }
                .filter { it.entity != null }
                .blockingFirst()

        assert(actual).isNotNull {
            it.prop(EntityEvent<Widget>::watchEvent)
                    .prop(WatchEvent<*>::kind)
                    .isEqualTo(StandardWatchEventKinds.ENTRY_MODIFY)
            it.prop(EntityEvent<Widget>::entity).isNotNull {
                it.isEqualTo(changed)
            }
        }
    }

    @Test
    fun itShouldWatchEntityDeleted() {
        val database = Database(folder.root, Widget::class)
        val widget = SampleDb.Widgets.One

        val actual = database.watchListing<Widget>()
                .doOnSubscribe { Single.just(widget)
                        .observeOn(Schedulers.io())
                        .delay(20, TimeUnit.MILLISECONDS)
                        .subscribe { widget -> database.remove(widget) }
                }
                .blockingFirst()

        assert(actual).isNotNull {
            it.prop(EntityEvent<Widget>::watchEvent)
                    .prop(WatchEvent<*>::kind)
                    .isEqualTo(StandardWatchEventKinds.ENTRY_DELETE)
            it.prop(EntityEvent<Widget>::entity).isNull()
        }
    }

}