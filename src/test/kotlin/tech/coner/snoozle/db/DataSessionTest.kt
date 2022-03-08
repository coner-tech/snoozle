package tech.coner.snoozle.db

import assertk.all
import assertk.assertThat
import assertk.assertions.isNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.sample.Gadget
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.sample.Subwidget
import tech.coner.snoozle.db.sample.Widget
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path


class DataSessionTest {

    @TempDir
    lateinit var root: Path

    private lateinit var session: DataSession

    @BeforeEach
    fun before() {
        session = SampleDatabaseFixture
            .factory(
                root = root,
                version = SampleDatabaseFixture.VERSION_HIGHEST
            )
            .openDataSession()
            .getOrThrow()
    }

    @AfterEach
    fun after() {
        session.closeAndAssertSuccess()
    }

    @Test
    fun itShouldLookUpEntityResources() {
        assertThat(session).all {
            transform("widget resource") { it.entity<Widget.Key, Widget>() }.isNotNull()
            transform("subwidget resource") { it.entity<Subwidget.Key, Subwidget>() }.isNotNull()
            transform("gadget resource") { it.entity<Gadget.Key, Gadget>() }.isNotNull()
        }
    }

}