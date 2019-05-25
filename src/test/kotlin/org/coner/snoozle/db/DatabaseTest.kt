package org.coner.snoozle.db

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import assertk.assertions.isSameAs
import org.coner.snoozle.db.sample.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path


class DatabaseTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
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