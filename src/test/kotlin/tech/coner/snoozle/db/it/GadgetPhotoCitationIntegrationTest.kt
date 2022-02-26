package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.hasLineCount
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.blob.BlobResource
import tech.coner.snoozle.db.sample.Gadget
import tech.coner.snoozle.db.sample.GadgetPhotoCitation
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path
import java.time.ZonedDateTime

class GadgetPhotoCitationIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var session: DataSession
    private lateinit var resource: BlobResource<GadgetPhotoCitation>

    @BeforeEach
    fun before() {
        session = SampleDatabaseFixture
            .factory(
                root = root,
                version = SampleDatabaseFixture.VERSION_HIGHEST
            )
            .openDataSession()
            .getOrThrow()
        resource = session.blob()
    }

    @AfterEach
    fun after() {
        session.close()
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
                gadgetId = SampleDatabaseFixture.Gadgets.GadgetOne.id,
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
        session.entity<Gadget.Key, Gadget>().create(gadgetTwo)
        val firstCitation = GadgetPhotoCitation(
                gadgetId = gadgetTwo.id,
                id = "first citation"
        )

        resource.put(firstCitation, "foo")

        val actualText = resource.getAsText(firstCitation)
        assertThat(actualText).isEqualTo("foo")
    }
}