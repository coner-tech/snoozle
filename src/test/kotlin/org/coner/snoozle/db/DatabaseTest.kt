package org.coner.snoozle.db

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
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
        assertThat(database.entities.entityResources).all {
            isNotNull()
            hasSize(3)
        }
    }

}