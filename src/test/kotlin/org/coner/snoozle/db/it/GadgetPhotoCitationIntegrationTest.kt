package org.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.hasLineCount
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import org.coner.snoozle.db.blob.BlobResource
import org.coner.snoozle.db.sample.Gadget
import org.coner.snoozle.db.sample.GadgetPhotoCitation
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.ZonedDateTime
import kotlin.streams.toList

class GadgetPhotoCitationIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase
    private lateinit var resource: BlobResource<GadgetPhotoCitation>

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
        resource = database.blob()
    }

    @Test
    fun `It should stream gadget photo citations`() {
        val actual = resource.stream().toList()

        assertThat(actual).hasSize(2)

        val texts = actual.map { resource.getAsText(it) }
        assertThat(texts).all {
            hasSize(2)
            index(0).hasLineCount(3)
            index(1).hasLineCount(3)
        }
    }

    @Test
    fun `It should put gadget photo citation for Gadget with existing PhotoCitations`() {
        val blob = GadgetPhotoCitation(
                gadgetId = SampleDb.Gadgets.GadgetOne.id,
                id = "close-up-photography-of-smartphone-beside-binder-clip-1841841"
        )

        resource.put(blob, "foo")

        val actualText = resource.getAsText(blob)
        assertThat(actualText).isEqualTo("foo")
    }

    @Test
    fun `It should put GadgetPhotoCitation for Gadget without PhotoCitations`() {
        val gadgetTwo = Gadget(
                name = "Gadget Without Photo Citations",
                silly = ZonedDateTime.parse("2020-01-01T12:09:00-05:00")
        )
        database.entity<Gadget.Key, Gadget>().create(gadgetTwo)
        val firstCitation = GadgetPhotoCitation(
                gadgetId = gadgetTwo.id,
                id = "first citation"
        )

        resource.put(firstCitation, "foo")

        val actualText = resource.getAsText(firstCitation)
        assertThat(actualText).isEqualTo("foo")
    }
}