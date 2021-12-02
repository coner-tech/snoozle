package tech.coner.snoozle.db

import assertk.all
import assertk.assertThat
import assertk.assertions.isNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.sample.*
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
    fun itShouldLookUpEntityResources() {
        assertThat(database).all {
            transform("widget resource") { it.entity<Widget.Key, Widget>() }.isNotNull()
            transform("subwidget resource") { it.entity<Subwidget.Key, Subwidget>() }.isNotNull()
            transform("gadget resource") { it.entity<Gadget.Key, Gadget>() }.isNotNull()
        }
    }

}