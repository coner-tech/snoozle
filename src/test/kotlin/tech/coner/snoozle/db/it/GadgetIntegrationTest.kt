package tech.coner.snoozle.db.it

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import tech.coner.snoozle.db.closeAndAssertSuccess
import tech.coner.snoozle.db.entity.EntityIoException
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.sample.Gadget
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path
import kotlin.io.path.readText

class GadgetIntegrationTest {

    @TempDir
    lateinit var root: Path
    private lateinit var session: DataSession
    private lateinit var resource: EntityResource<Gadget.Key, Gadget>

    @BeforeEach
    fun before() {
        session = SampleDatabaseFixture
            .factory(
                root = root,
                version = SampleDatabaseFixture.VERSION_HIGHEST
            )
            .openDataSession()
            .getOrThrow()
        resource = session.entity()
    }

    @AfterEach
    fun after() {
        session.closeAndAssertSuccess()
    }

    @Test
    fun `It should create gadget`() {
        val create = Gadget(name = "Create")

        resource.create(create)

        val expectedFileContents = SampleDatabaseFixture.Gadgets.asJson(create)
        val actualFileContents = SampleDatabaseFixture.Gadgets.tempFile(root, create).readText()
        JSONAssert.assertEquals(expectedFileContents, actualFileContents, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should update gadget`() {
        val original = SampleDatabaseFixture.Gadgets.GadgetOne
        val update = original.copy(name = "Update")

        resource.update(update)

        val expectedFileContents = SampleDatabaseFixture.Gadgets.asJson(update)
        val actualFileContents = SampleDatabaseFixture.Gadgets.tempFile(root, update).readText()
        JSONAssert.assertEquals(expectedFileContents, actualFileContents, JSONCompareMode.LENIENT)
    }

    @Test
    fun `it should stream Gadgets`() {
        val all = SampleDatabaseFixture.Gadgets.all

        val actual = resource.stream()
                .toList()
                .sortedBy { it.name }

        assertThat(actual).isEqualTo(all)
    }

    @Test
    fun `It should delete gadget by key`() {
        val gadgetOne = SampleDatabaseFixture.Gadgets.GadgetOne
        val actualFile = SampleDatabaseFixture.Gadgets.tempFile(root, gadgetOne)
        Assumptions.assumeThat(actualFile).exists()

        resource.delete(gadgetOne)

        Assertions.assertThat(actualFile).doesNotExist()
    }

    @Test
    fun `It should throw when delete called with key that doesn't exist`() {
        val doesNotExist = Gadget(name = "does not exist")
        val tempFile = SampleDatabaseFixture.Gadgets.tempFile(root, doesNotExist)
        Assumptions.assumeThat(tempFile).doesNotExist()

        assertThrows<EntityIoException.NotFound> {
            resource.delete(doesNotExist)
        }
    }
}