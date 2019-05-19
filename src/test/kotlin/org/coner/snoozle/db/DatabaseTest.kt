package org.coner.snoozle.db

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import assertk.assertions.isSameAs
import org.coner.snoozle.db.sample.*
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
        assertThat(database.resources).all {
            isNotNull()
            hasSize(3)
        }
        assertThat(database.findResource<Widget>().entityDefinition.kClass).isSameAs(Widget::class)
        assertThat(database.findResource<Subwidget>().entityDefinition.kClass).isSameAs(Subwidget::class)
        assertThat(database.findResource<Gadget>().entityDefinition.kClass).isSameAs(Gadget::class)
    }

}