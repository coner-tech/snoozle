package org.coner.snoozle.db.it

import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToWithGivenProperties
import assertk.assertions.prop
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.assertj.core.api.SoftAssertions
import org.coner.snoozle.db.WholeRecord
import org.coner.snoozle.db.sample.Gadget
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.time.ZonedDateTime
import kotlin.reflect.KProperty1

class GadgetIntegrationTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private lateinit var database: SampleDatabase

    @Before
    fun before() {
        database = SampleDb.factory(folder)
    }

    @Test
    fun itShouldWriteOriginalWithCurrentVersionZero() {
        val original = Gadget(name = "Original")

        database.put(original)

        val path = SampleDb.Gadgets.tempFile(folder, original)
        val expected = """
            {
                "currentVersion": {
                    "version": 0
                },
                "history": []
            }
        """.trimIndent()
        JSONAssert.assertEquals(expected, path.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldWriteFirstRevision() {
        val original = Gadget(name = "Original")
        database.put(original)
        val firstRevision = original.copy(name = "First Revision")

        database.put(firstRevision)

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
        val path = SampleDb.Gadgets.tempFile(folder, firstRevision)
        JSONAssert.assertEquals(expected, path.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldWriteTimestampAsIsoString() {
        val timestampAsString = "2019-05-18T20:55:01-04:00"
        val gadget = Gadget(silly = ZonedDateTime.parse(timestampAsString))

        database.put(gadget)

        val file = SampleDb.Gadgets.tempFile(folder, gadget)
        val expected = """
            {
                "entity": {
                    "silly": "$timestampAsString"
                }
            }
        """.trimIndent()
        JSONAssert.assertEquals(expected, file.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldWriteSecondRevision() {
        val original = Gadget(name = "Original")
        database.put(original)
        val firstRevision = original.copy(name = "First Revision")
        database.put(firstRevision)

        val secondRevision = firstRevision.copy(name = "Second Revision")
        database.put(secondRevision)

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
        val path = SampleDb.Gadgets.tempFile(folder, secondRevision)
        JSONAssert.assertEquals(expected, path.readText(), JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldGetWholeRecord() {
        val expected = SampleDb.Gadgets.GadgetOneWholeRecord
        Assumptions.assumeThat(expected.entityValue).isEqualTo(SampleDb.Gadgets.GadgetOne)

        val actual = database.getWholeRecord(Gadget::id to expected.entityValue.id)

        Assertions.assertThat(actual)
                .isEqualToIgnoringGivenFields(
                        expected,
                        "entityObjectNode",
                        "history"
                )
        Assertions.assertThat(actual.history)
                .isNotNull
                .hasSameSizeAs(expected.history)
        Assertions.assertThat(actual.history!![0]).isEqualToIgnoringGivenFields(expected.history!![0], "entityObjectNode")
        Assertions.assertThat(actual.history!![1]).isEqualToIgnoringGivenFields(expected.history!![1], "entityObjectNode")
    }
}