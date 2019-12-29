package org.coner.snoozle.db

import assertk.all
import assertk.assertThat
import assertk.assertions.isNotNull
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
    fun itShouldLookUpEntityResources() {
        assertThat(database).all {
            transform("widget resource") { it.entity<Widget>() }.isNotNull()
            transform("subwidget resource") { it.entity<Subwidget>() }.isNotNull()
            transform("gadget resource") { it.entity<Gadget>() }.isNotNull()
        }
    }

}