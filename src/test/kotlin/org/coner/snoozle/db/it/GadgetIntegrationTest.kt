package org.coner.snoozle.db.it

import assertk.assertAll
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
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.nio.file.Files
import java.nio.file.Path
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
        val path = SampleDb.Gadgets.tempFile(root, original, 0)
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

        val actualReturnValue = resource.put(original, VersionArgument.Auto)

        assertThat(actualReturnValue, "actual returned container").version().isEqualTo(0)
        assertThat(path).transform { it.toFile() }.exists()
        JSONAssert.assertEquals(expectedFileContents, path.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should write first revision with highest version argument`() {
        val original = Gadget(name = "Original")
        resource.put(original, VersionArgument.Manual(0))
        val firstRevision = original.copy(name = "First Revision")
        val path = SampleDb.Gadgets.tempFile(root, firstRevision, 1)
        val expectedFileContents = """
            {
                "entity": {
                    "id": "${firstRevision.id}",
                    "name": "First Revision",
                    "silly": null
                },
                "version": 1
            }
        """.trimIndent()

        val actual = resource.put(firstRevision, VersionArgument.Manual(1))

        assertThat(actual, "actual returned container").version().isEqualTo(1)
        assertThat(path).transform { it.toFile() }.exists()
        JSONAssert.assertEquals(expectedFileContents, path.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should write first revision with auto version argument`() {
        val original = Gadget(name = "Original")
        resource.put(original, VersionArgument.Manual(0))
        val firstRevision = original.copy(name = "First Revision")
        val path = SampleDb.Gadgets.tempFile(root, firstRevision, 1)
        val expectedFileContents = """
            {
                "entity": {
                    "id": "${firstRevision.id}",
                    "name": "First Revision",
                    "silly": null
                },
                "version": 1
            }
        """.trimIndent()

        val actual = resource.put(firstRevision, VersionArgument.Auto)

        assertThat(actual, "actual returned container").version().isEqualTo(1)
        assertThat(path).transform { it.toFile() }.exists()
        JSONAssert.assertEquals(expectedFileContents, path.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should throw when put given entity with manual version not incremented correctly`() {
        val original = Gadget(name = "Original")
        resource.put(original, VersionArgument.Manual(0))
        val firstRevision = original.copy(name = "First Revision")

        val actual = assertThrows<IllegalArgumentException> {
            resource.put(firstRevision, VersionArgument.Manual(1234 /* wrong */))
        }

        assertThat(actual).messageContains("increment")
    }

    @Test
    fun `It should write second revision`() {
        val original = Gadget(name = "Original")
        resource.put(original, VersionArgument.Manual(0))
        val firstRevision = original.copy(name = "First Revision")
        resource.put(firstRevision, VersionArgument.Manual(1))
        val secondRevision = firstRevision.copy(name = "Second Revision")
        val path = SampleDb.Gadgets.tempFile(root, secondRevision, 2)
        val expectedFileContents = """
            {
                "entity": {
                    "id": "${secondRevision.id}",
                    "name": "Second Revision",
                    "silly": null
                },
                "version": 2
            }
        """.trimIndent()

        val actual = resource.put(secondRevision, VersionArgument.Manual(2))

        assertThat(actual, "actual returned container").version().isEqualTo(2)
        assertThat(path).transform { it.toFile() }.exists()
        JSONAssert.assertEquals(expectedFileContents, path.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun `It should get versions`() {
        val gadgetOne = SampleDb.Gadgets.GadgetOne
        val expected = SampleDb.Gadgets.GadgetOneVersions
        Assumptions.assumeThat(expected.last().entity).isEqualTo(SampleDb.Gadgets.GadgetOne)

        val actual = resource.getAllVersionsOfEntity(gadgetOne.id)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `It should get the highest version`() {
        val gadgetOne = SampleDb.Gadgets.GadgetOne

        val actual = resource.getEntity(gadgetOne.id)

        assertThat(actual).version().isEqualTo(2)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2])
    fun `It should get specific versions`(version: Int) {
        val gadgetVersionContainer = SampleDb.Gadgets.GadgetOneVersions[version]

        val actual = resource.getEntity(SampleDb.Gadgets.GadgetOne.id, VersionArgument.Manual(version))

        assertThat(actual).isEqualTo(gadgetVersionContainer)
    }

    @Test
    fun `it should list all by highest version`() {
        val allHighestVersions = SampleDb.Gadgets.allByHighestVersion

        val actual = resource.streamAll().toList()

        assertThat(actual).isEqualTo(allHighestVersions)
    }

    @Test
    fun `It should delete when given correct version`() {
        val gadgetOne = SampleDb.Gadgets.GadgetOneVersions.last()
        val gadgetOneTempFiles = SampleDb.Gadgets.GadgetOneVersions.map {
            SampleDb.Gadgets.tempFile(root, it.entity, it.version)
        }
        val gadgetOneVersionListing = gadgetOneTempFiles.first().parent
        Assumptions.assumeThat(gadgetOneTempFiles).allMatch { Files.exists(it) }
        Assumptions.assumeThat(gadgetOneVersionListing).exists()

        resource.delete(gadgetOne.entity, VersionArgument.Manual(gadgetOne.version))

        assertAll {
            assertThat(gadgetOneTempFiles).each {
                it.matchesPredicate { path -> Files.notExists(path) }
            }
            assertThat(gadgetOneVersionListing).matchesPredicate { path -> Files.notExists(path) }
        }
    }

    @Test
    fun `It should throw when delete called with entity that doesn't exist`() {
        val doesNotExist = Gadget(name = "does not exist")
        val tempFile = SampleDb.Gadgets.tempFile(root, doesNotExist, 0)
        Assumptions.assumeThat(tempFile).doesNotExist()

        assertThrows<EntityIoException.NotFound> {
            resource.delete(doesNotExist, VersionArgument.Manual(0))
        }
    }

    @Test
    fun `It should throw when delete called with version lower than highest`() {
        val gadgetOneOriginal = SampleDb.Gadgets.GadgetOneVersions.first()
        
        assertThrows<IllegalArgumentException> {
            resource.delete(gadgetOneOriginal.entity, VersionArgument.Manual(gadgetOneOriginal.version))
        }
    }
}