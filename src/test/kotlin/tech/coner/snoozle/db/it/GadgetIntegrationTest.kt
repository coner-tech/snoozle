package tech.coner.snoozle.db.it

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import tech.coner.snoozle.db.entity.EntityIoException
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.sample.Gadget
import tech.coner.snoozle.db.sample.SampleDatabase
import tech.coner.snoozle.db.sample.SampleDb
import tech.coner.snoozle.util.readText
import java.nio.file.Path

class GadgetIntegrationTest {

    @TempDir
    lateinit var root: Path
    private lateinit var database: SampleDatabase
    private lateinit var resource: EntityResource<Gadget.Key, Gadget>

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
        resource = database.entity()
    }

    @Test
    fun `It should create gadget`() {
        val create = Gadget(name = "Create")

        resource.create(create)

        val expectedFileContents = SampleDb.Gadgets.asJson(create)
        val actualFileContents = SampleDb.Gadgets.tempFile(root, create).readText()
        JSONAssert.assertEquals(expectedFileContents, actualFileContents, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should update gadget`() {
        val original = SampleDb.Gadgets.GadgetOne
        val update = original.copy(name = "Update")

        resource.update(update)

        val expectedFileContents = SampleDb.Gadgets.asJson(update)
        val actualFileContents = SampleDb.Gadgets.tempFile(root, update).readText()
        JSONAssert.assertEquals(expectedFileContents, actualFileContents, JSONCompareMode.LENIENT)
    }

    @Test
    fun `it should stream Gadgets`() {
        val all = SampleDb.Gadgets.all

        val actual = resource.stream()
                .toList()
                .sortedBy { it.name }

        assertThat(actual).isEqualTo(all)
    }

    @Test
    fun `It should delete gadget by key`() {
        val gadgetOne = SampleDb.Gadgets.GadgetOne
        val actualFile = SampleDb.Gadgets.tempFile(root, gadgetOne)
        Assumptions.assumeThat(actualFile).exists()

        resource.delete(gadgetOne)

        Assertions.assertThat(actualFile).doesNotExist()
    }

    @Test
    fun `It should throw when delete called with key that doesn't exist`() {
        val doesNotExist = Gadget(name = "does not exist")
        val tempFile = SampleDb.Gadgets.tempFile(root, doesNotExist)
        Assumptions.assumeThat(tempFile).doesNotExist()

        assertThrows<EntityIoException.NotFound> {
            resource.delete(doesNotExist)
        }
    }
}