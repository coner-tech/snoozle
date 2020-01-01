package org.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.hasLineCount
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import org.coner.snoozle.db.sample.Gadget
import org.coner.snoozle.db.sample.GadgetPhotoCitation
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.ZonedDateTime

class GadgetPhotoCitationIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
    }

    @Test
    fun itShouldGetGadgetPhotoCitations() {
        val actual = database.blob<GadgetPhotoCitation>().list(SampleDb.Gadgets.GadgetOne.id)

        assertThat(actual).hasSize(2)

        val texts = actual
                .map { database.blob<GadgetPhotoCitation>().getAsText(it) }
        assertThat(texts).all {
            hasSize(2)
            index(0).hasLineCount(3)
            index(1).hasLineCount(3)
        }
    }

    @Test
    fun itShouldPutGadgetPhotoCitationForGadgetWithExistingPhotoCitations() {
        val blob = GadgetPhotoCitation(
                gadgetId = SampleDb.Gadgets.GadgetOne.id,
                id = "close-up-photography-of-smartphone-beside-binder-clip-1841841"
        )

        database.blob<GadgetPhotoCitation>().put(blob, "foo")

        assertThat(database.blob<GadgetPhotoCitation>().getAsText(blob)).isEqualTo("foo")
    }

    @Test
    fun itShouldPutGadgetPhotoCitationForGadgetWithoutPhotoCitations() {
        val gadgetTwo = Gadget(
                name = "Gadget Without Photo Citations",
                silly = ZonedDateTime.parse("2020-01-01T12:09:00-05:00")
        )
        database.entity<Gadget>().put(gadgetTwo)
        val firstCitation = GadgetPhotoCitation(
                gadgetId = gadgetTwo.id,
                id = "first citation"
        )

        database.blob<GadgetPhotoCitation>().put(firstCitation, "foo")

        assertThat(database.blob<GadgetPhotoCitation>().getAsText(firstCitation)).isEqualTo("foo")
    }
}