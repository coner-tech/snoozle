package org.coner.snoozle.db.it

import org.assertj.core.api.Assertions.assertThat
import org.coner.snoozle.db.Database
import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.Widget
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.reflect.KClass

class DatabaseIntegrationTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    @Before
    fun before() {
        SampleDb.copyTo(folder)
    }

    @Test
    fun itShouldInitResourcesWithItsFirstConstructor() {
        val database = Database(
                root = folder.root,
                entities = *arrayOf(Widget::class, Subwidget::class)
        )

        assertThat(database.resources)
                .isNotNull
                .hasSize(2)
        assertThat(database.resources[Widget::class as KClass<Entity>])
                .extracting { it?.kclass }
                .isSameAs(Widget::class)
        assertThat(database.resources[Subwidget::class as KClass<Entity>])
                .extracting { it?.kclass }
                .isSameAs(Subwidget::class)
    }

}