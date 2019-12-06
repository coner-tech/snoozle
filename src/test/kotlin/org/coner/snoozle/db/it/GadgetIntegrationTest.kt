package org.coner.snoozle.db.it

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.snoozle.db.sample.Gadget
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.coner.snoozle.util.readText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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
        TODO()
//        val original = Gadget(name = "Original")
//
//        database.put(original)
//
//        val path = SampleDb.Gadgets.tempFile(root, original)
//        val expected = """
//            {
//                "currentVersion": {
//                    "version": 0
//                },
//                "history": []
//            }
//        """.trimIndent()
//        val actual = path.readText()
//        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT)
    }

    @Test
    fun itShouldWriteFirstRevision() {
        TODO()
//        val original = Gadget(name = "Original")
//        database.put(original)
//        val firstRevision = original.copy(name = "First Revision")
//
//        database.put(firstRevision)
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
    fun itShouldWriteTimestampAsIsoString() {
        TODO()
//        val timestampAsString = "2019-05-18T20:55:01-04:00"
//        val gadget = Gadget(silly = ZonedDateTime.parse(timestampAsString))
//
//        database.put(gadget)
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
        TODO()
//        val original = Gadget(name = "Original")
//        database.put(original)
//        val firstRevision = original.copy(name = "First Revision")
//        database.put(firstRevision)
//
//        val secondRevision = firstRevision.copy(name = "Second Revision")
//        database.put(secondRevision)
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
    fun itShouldGetWholeRecord() {
        TODO()
//        val expected = SampleDb.Gadgets.GadgetOneWholeRecord
//        Assumptions.assumeThat(expected.entityValue).isEqualTo(SampleDb.Gadgets.GadgetOne)
//
//        val actual = database.getWholeRecord(Gadget::id to expected.entityValue.id)
//
//        Assertions.assertThat(actual)
//                .isEqualToIgnoringGivenFields(
//                        expected,
//                        "_entityObjectNode",
//                        "history"
//                )
//        Assertions.assertThat(actual.history)
//                .isNotNull
//                .hasSameSizeAs(expected.history)
//        Assertions.assertThat(actual.history!![0]).isEqualToIgnoringGivenFields(expected.history!![0], "_entityObjectNode")
//        Assertions.assertThat(actual.history!![1]).isEqualToIgnoringGivenFields(expected.history!![1], "_entityObjectNode")
    }
}