package org.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.assertj.core.api.Assumptions
import org.coner.snoozle.db.entity.*
import org.coner.snoozle.db.sample.Gadget
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.util.readText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.nio.file.Path
import java.time.ZonedDateTime
import java.util.stream.Collectors
import kotlin.streams.toList

class GadgetIntegrationTest {

    @TempDir
    lateinit var root: Path
    private lateinit var database: SampleDatabase
    private lateinit var resource: VersionedEntityResource<Gadget>

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
        resource = database.versionedEntity()
    }

    @Test
    fun `It should write original with current version zero`() {
        val original = Gadget(name = "Original")

        val actualReturnValue = resource.put(original, VersionArgument.Auto)

        assertThat(actualReturnValue, "actual return value").all {
            version().isEqualTo(0)
        }
        val path = SampleDb.Gadgets.tempFile(root, original, 0)
        assertThat(path).transform { it.toFile() }.exists()
        val expectedFileContents = """
            {
                "entity": {
                    "id": "${original.id}",
                    "name": "Original",
                    "silly": null
                },
                "version": 0
            }
        """.trimIndent()
        JSONAssert.assertEquals(expectedFileContents, path.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should write first revision with highest version argument`() {
        TODO()
    }

    @Test
    fun `It should write first revision with specific version argument`() {
//        val original = Gadget(name = "Original")
//        resource.put(original, VersionArgument.Auto)
//        val firstRevision = original.copy(name = "First Revision")
//
//        resource.put(firstRevision, VersionArgument.Manual(1))
//
//        val expected = """
//            {
//                "currentVersion": {
//                    "version": 1
//                },
//                "history": [
//                    {
//                        "entity": {
//                            "id": ${original.id},
//                            "name": "Original",
//                            "silly": null
//                        },
//                        "version": 0
//                    }
//                ]
//            }
//        """.trimIndent()
//        val path = SampleDb.Gadgets.tempFile(root, firstRevision)
//        val actual = path.readText()
//        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should fail to write revisions with invalid specific version arguments`() {
        TODO()
    }

    @Test
    fun itShouldWriteTimestampAsIsoString() {
//        val timestampAsString = "2019-05-18T20:55:01-04:00"
//        val gadget = Gadget(silly = ZonedDateTime.parse(timestampAsString))
//
//        resource.put(gadget, VersionArgument.Auto)
//
//        val file = SampleDb.Gadgets.tempFile(root, gadget)
//        val expected = """
//            {
//                "entity": {
//                    "silly": "$timestampAsString"
//                }
//            }
//        """.trimIndent()
//        val actual = file.readText()
//        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldWriteSecondRevision() {
//        val original = Gadget(name = "Original")
//        resource.put(original, VersionArgument.Manual(0))
//        val firstRevision = original.copy(name = "First Revision")
//        resource.put(firstRevision, VersionArgument.Manual(1))
//
//        val secondRevision = firstRevision.copy(name = "Second Revision")
//        resource.put(secondRevision, VersionArgument.Manual(2))
//
//        val expected = """
//            {
//                ${SampleDb.Gadgets.asJson(secondRevision)},
//                "currentVersion": {
//                    "version": 2
//                },
//                "history": [
//                    {
//                        ${SampleDb.Gadgets.asJson(original)},
//                        "version": 0
//                    },
//                    {
//                        ${SampleDb.Gadgets.asJson(firstRevision)},
//                        "version": 1
//                    }
//                ]
//            }
//        """.trimIndent()
//        val path = SampleDb.Gadgets.tempFile(root, secondRevision)
//        val actual = path.readText()
//        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldGetVersions() {
        val gadgetOne = SampleDb.Gadgets.GadgetOne
        val expected = SampleDb.Gadgets.GadgetOneVersions
        Assumptions.assumeThat(expected.last().entity).isEqualTo(SampleDb.Gadgets.GadgetOne)

        val actual = resource.getAllVersionsOfEntity(gadgetOne.id)

        assertThat(actual).isEqualTo(expected)
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

        val actual = resource.getEntity(SampleDb.Gadgets.GadgetOne.id, VersionArgument.Manual(version))

        assertThat(actual).isEqualTo(gadgetVersionContainer)
    }

    @Test
    fun `it should list all by highest version`() {
        val allHighestVersions = SampleDb.Gadgets.allByHighestVersion
        val resource = database.versionedEntity<Gadget>()

        val actual = resource.listAll().toList()

        assertThat(actual).isEqualTo(allHighestVersions)
    }
}