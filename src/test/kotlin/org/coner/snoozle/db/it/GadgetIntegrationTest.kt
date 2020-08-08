package org.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.snoozle.db.entity.VersionArgument
import org.coner.snoozle.db.entity.entity
import org.coner.snoozle.db.entity.version
import org.coner.snoozle.db.sample.Gadget
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.util.readText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.nio.file.Path
import java.time.ZonedDateTime

class GadgetIntegrationTest {

    @TempDir
    lateinit var root: Path
    private lateinit var database: SampleDatabase

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
    }

    @Test
    fun itShouldWriteOriginalWithCurrentVersionZero() {
        val original = Gadget(name = "Original")

        database.versionedEntity<Gadget>().put(original, VersionArgument.New)

        val path = SampleDb.Gadgets.tempFile(root, original)
        TODO("read output json and assert")
    }

    @Test
    fun itShouldWriteFirstRevision() {
        val resource = database.versionedEntity<Gadget>()
        val original = Gadget(name = "Original")
        resource.put(original, VersionArgument.New)
        val firstRevision = original.copy(name = "First Revision")

        resource.put(firstRevision, VersionArgument.Specific(2))

        val expected = """
            {
                "currentVersion": {
                    "version": 1
                },
                "history": [
                    {
                        "entity": {
                            "id": ${original.id},
                            "name": "Original",
                            "silly": null
                        },
                        "version": 0
                    }
                ]
            }
        """.trimIndent()
        val path = SampleDb.Gadgets.tempFile(root, firstRevision)
        val actual = path.readText()
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldWriteTimestampAsIsoString() {
        val timestampAsString = "2019-05-18T20:55:01-04:00"
        val gadget = Gadget(silly = ZonedDateTime.parse(timestampAsString))

        database.entity<Gadget>().put(gadget)

        val file = SampleDb.Gadgets.tempFile(root, gadget)
        val expected = """
            {
                "entity": {
                    "silly": "$timestampAsString"
                }
            }
        """.trimIndent()
        val actual = file.readText()
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldWriteSecondRevision() {
        val original = Gadget(name = "Original")
        database.entity<Gadget>().put(original)
        val firstRevision = original.copy(name = "First Revision")
        database.entity<Gadget>().put(firstRevision)

        val secondRevision = firstRevision.copy(name = "Second Revision")
        database.entity<Gadget>().put(secondRevision)

        val expected = """
            {
                ${SampleDb.Gadgets.asJson(secondRevision)},
                "currentVersion": {
                    "version": 2
                },
                "history": [
                    {
                        ${SampleDb.Gadgets.asJson(original)},
                        "version": 0
                    },
                    {
                        ${SampleDb.Gadgets.asJson(firstRevision)},
                        "version": 1
                    }
                ]
            }
        """.trimIndent()
        val path = SampleDb.Gadgets.tempFile(root, secondRevision)
        val actual = path.readText()
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldGetVersions() {
        val gadgetOne = SampleDb.Gadgets.GadgetOne
        val expected = SampleDb.Gadgets.GadgetOneVersions
        Assumptions.assumeThat(expected.last().entity).isEqualTo(SampleDb.Gadgets.GadgetOne)

        val actual = database.versionedEntity<Gadget>().getAllVersionsOfEntity(gadgetOne.id)

        Assertions.assertThat(actual).hasSize(3)
    }

    @Test
    fun `It should get the highest version`() {
        val gadgetOne = SampleDb.Gadgets.GadgetOne
        val resource = database.versionedEntity<Gadget>()

        val actual = resource.getEntity(gadgetOne.id)

        assertThat(actual).version().isEqualTo(2)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2])
    fun `It should get specific versions`(version: Int) {
        val gadgetVersionContainer = SampleDb.Gadgets.GadgetOneVersions[version]
        val resource = database.versionedEntity<Gadget>()

        val actual = resource.getEntity(SampleDb.Gadgets.GadgetOne.id, VersionArgument.Specific(version))

        assertThat(actual).isEqualTo(gadgetVersionContainer)
    }
}