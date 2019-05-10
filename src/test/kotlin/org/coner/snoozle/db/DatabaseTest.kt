package org.coner.snoozle.db

import assertk.all
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import assertk.assertions.isSameAs
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.Widget
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private lateinit var database: SampleDatabase

    @Before
    fun before() {
        database = SampleDb.factory(folder)
    }

    @Test
    fun itShouldInitResourcesWithItsFirstConstructor() {
        assertk.assertThat(database.resources).all {
            isNotNull()
            hasSize(2)
        }
        assertk.assertThat(database.findResource<Widget>().entityDefinition.kClass).isSameAs(Widget::class)
        assertk.assertThat(database.findResource<Subwidget>().entityDefinition.kClass).isSameAs(Subwidget::class)
    }

}