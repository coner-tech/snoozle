package tech.coner.snoozle.db.it

import assertk.Assert
import assertk.all
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import tech.coner.snoozle.db.closeAndAssertSuccess
import tech.coner.snoozle.db.migration.MigrationException
import tech.coner.snoozle.db.sample.SampleDatabase
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.sample.SampleDatabaseFixture.Subwidgets
import tech.coner.snoozle.db.sample.SampleDatabaseFixture.Widgets
import tech.coner.snoozle.db.sample.Subwidget
import tech.coner.snoozle.db.sample.Widget
import tech.coner.snoozle.db.session.administrative.AdministrativeSession
import tech.coner.snoozle.util.doesNotExist
import tech.coner.snoozle.util.snoozleJacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.readText
import tech.coner.snoozle.db.path.asAbsolute

class MigrationIntegrationTest {

    @Nested
    inner class MigrateSampleDatabaseVersionNullTo1 {

        @TempDir lateinit var rootVNull: Path
        @TempDir lateinit var rootV1: Path

        lateinit var session: AdministrativeSession

        @BeforeEach
        fun before() {
            session = SampleDatabaseFixture
                .factory(
                    root = rootVNull,
                    version = null
            )
                .openAdministrativeSession()
                .getOrThrow()
            SampleDatabaseFixture.factory(
                root = rootV1,
                version = 1
            )
        }

        @AfterEach
        fun after() {
            session.closeAndAssertSuccess()
        }

        @Test
        fun `It should write upgraded database version`() {
            val actual = session.migrateDatabase(null to 1)

            assertAll {
                assertThat(actual).isSuccess()
                assertThat(rootVNull.fromRootResolveSnoozleDatabaseVersion()).textContentsAreEqualTo("1")
            }
        }

        @Test
        fun `It should move widgets`() {
            val actual = session.migrateDatabase(null to 1)

            assertAll {
                assertThat(actual).isSuccess()
                val after = rootVNull.findAllAsRelative()
                assertThat(after).isNotNull().all {
                    containsAll(
                        Paths.get("widgets"),
                        Paths.get("widgets", "1f30d7b6-0296-489a-9615-55868aeef78a.json"),
                        Paths.get("widgets", "94aa3940-1183-4e91-b329-d9dc9c688540.json"),
                    )
                }
            }
        }

        @Test
        fun `It should move subwidgets`() {
            val actual = session.migrateDatabase(null to 1)

            assertAll {
                assertThat(actual).isSuccess()
                val after = rootVNull.findAllAsRelative()
                assertThat(after).isNotNull().all {
                    containsAll(
                        Paths.get("widgets", "1f30d7b6-0296-489a-9615-55868aeef78a", "subwidgets"),
                        Paths.get("widgets","1f30d7b6-0296-489a-9615-55868aeef78a", "subwidgets", "220460be-27d4-4e6d-8ac3-34cf5139b229.json"),
                        Paths.get("widgets", "94aa3940-1183-4e91-b329-d9dc9c688540", "subwidgets"),
                        Paths.get("widgets", "94aa3940-1183-4e91-b329-d9dc9c688540", "subwidgets", "0dc69a13-b911-4c39-bf56-19fcdb7a8baf.json"),
                    )
                }
            }
        }

        @Test
        fun `It should add properties to widgets`() {
            val actual = session.migrateDatabase(null to 1)

            assertAll {
                assertThat(actual).isSuccess()
                val widgetOneActualJson = Widgets.tempFile(rootVNull, 1, Widgets.One.id).readText()
                val widgetOneExpectedJson = Widgets.tempFile(rootV1, 1, Widgets.One.id).readText()
                val widgetTwoActualJson = Widgets.tempFile(rootVNull, 1, Widgets.Two.id).readText()
                val widgetTwoExpectedJson = Widgets.tempFile(rootV1, 1, Widgets.Two.id).readText()
                JSONAssert.assertEquals(widgetOneExpectedJson, widgetOneActualJson, true)
                JSONAssert.assertEquals(widgetTwoExpectedJson, widgetTwoActualJson, true)
            }
        }

        @Test
        fun `It should auto migrate to version 3`() {
            val actual = session.autoMigrateDatabase()

            assertAll {
                assertThat(actual).isSuccess()
                assertThat(rootVNull.fromRootResolveSnoozleDatabaseVersion()).textContentsAreEqualTo("3")
            }
        }

        @Test
        fun `It should migrate incrementally to version 1`() {
            val actual = session.incrementalMigrateDatabase()

            assertAll {
                assertThat(actual).isSuccess()
                assertThat(rootVNull.fromRootResolveSnoozleDatabaseVersion()).textContentsAreEqualTo("1")
            }
        }

        @Test
        fun `When migrating segment with incorrect from, it should result in failure`() {
            val actual = session.migrateDatabase(1 to 2)

            assertThat(actual).all {
                isFailure()
                    .messageContains("Migration does not match current version")
            }
        }

        @Test
        fun `It should delete all subwidget folders`() {
            val actual = session.migrateDatabase(null to 1)

            assertAll {
                assertThat(actual).isSuccess()
                fun subwidgetFolder(subwidget: Subwidget): Path {
                    return rootVNull.resolve("widget").resolve("${subwidget.widgetId}").resolve("subwidget")
                }
                assertThat(subwidgetFolder(Subwidgets.WidgetOneSubwidgetOne)).doesNotExist()
                assertThat(subwidgetFolder(Subwidgets.WidgetTwoSubwidgetOne)).doesNotExist()
            }
        }

        @Test
        fun `It should delete all widget id folders`() {
            val actual = session.migrateDatabase(null to 1)

            assertAll {
                assertThat(actual).isSuccess()
                fun widgetIdFolder(widget: Widget): Path {
                    return rootVNull.resolve("widget").resolve("${widget.id}")
                }
                assertAll {
                    assertThat(widgetIdFolder(Widgets.One)).doesNotExist()
                    assertThat(widgetIdFolder(Widgets.Two)).doesNotExist()
                }
            }
        }

        @Test
        fun `It should delete widget folder`() {
            val actual = session.migrateDatabase(null to 1)

            assertAll {
                assertThat(actual).isSuccess()
                assertThat(rootVNull.resolve("widget")).doesNotExist()
            }
        }
    }

    @Nested
    inner class MigrateSampleDatabaseVersion1To2 {
        @TempDir lateinit var rootV1: Path
        @TempDir lateinit var rootV2: Path

        lateinit var session: AdministrativeSession

        @BeforeEach
        fun before() {
            session = SampleDatabaseFixture
                .factory(
                    root = rootV1,
                    version = 1
                )
                .openAdministrativeSession()
                .getOrThrow()
            SampleDatabaseFixture.factory(
                root = rootV2,
                version = 2
            )
            simulateApplicationAddedSomeItemsToWidgetObjectArray()
        }

        private fun simulateApplicationAddedSomeItemsToWidgetObjectArray() {
            val objectMapper = snoozleJacksonObjectMapper()
            fun ObjectMapper.addItemsToWidgetObjectArray(onVersion: Int, widgetId: UUID) {
                val root = when (onVersion) {
                    1 -> rootV1
                    2 -> rootV2
                    else -> throw IllegalArgumentException("onVersion must be 1 or 2")
                }
                val path = Widgets.tempFile(root, 2, widgetId)
                val tree = path
                    .bufferedReader()
                    .use { readTree(it) }
                val objectArray = tree.at("/object/array") as ArrayNode
                val objectArrayObjectZero = objectArray.addObject()
                if (onVersion == 2) {
                    objectArrayObjectZero.put("boolean", true)
                }
                path
                    .bufferedWriter()
                    .use { writeValue(it, tree) }
                check(
                    tree.at("/object/array/0")
                        .let { !it.isMissingNode && it.isObject }
                ) {
                    "Something went wrong simulating application added some items to widget object array at $path"
                }
            }
            objectMapper.addItemsToWidgetObjectArray(1, Widgets.One.id)
            objectMapper.addItemsToWidgetObjectArray(1, Widgets.Two.id)
            objectMapper.addItemsToWidgetObjectArray(2, Widgets.One.id)
            objectMapper.addItemsToWidgetObjectArray(2, Widgets.Two.id)
        }

        @Test
        fun `It should write upgraded database version`() {
            val actual = session.migrateDatabase(1 to 2)

            assertAll {
                assertThat(actual).isSuccess()
                assertThat(rootV1.fromRootResolveSnoozleDatabaseVersion()).textContentsAreEqualTo("2")
            }
        }

        @Test
        fun `It should add boolean property on widget object array item objects`() {
            val actual = session.migrateDatabase(1 to 2)

            assertAll {
                assertThat(actual).isSuccess()
                val widgetOneActualJson = Widgets.tempFile(rootV1, 2, Widgets.One.id).readText()
                val widgetTwoActualJson = Widgets.tempFile(rootV1, 2, Widgets.Two.id).readText()
                val widgetOneExpectedJson = Widgets.tempFile(rootV2, 2, Widgets.One.id).readText()
                val widgetTwoExpectedJson = Widgets.tempFile(rootV2, 2, Widgets.Two.id).readText()
                JSONAssert.assertEquals(widgetOneExpectedJson, widgetOneActualJson, true)
                JSONAssert.assertEquals(widgetTwoExpectedJson, widgetTwoActualJson, true)
            }
        }

        @Test
        fun `It should auto migrate to version 3`() {
            val actual = session.autoMigrateDatabase()

            assertAll {
                assertThat(actual).isSuccess()
                assertThat(rootV1.fromRootResolveSnoozleDatabaseVersion()).textContentsAreEqualTo("3")
            }
        }

        @Test
        fun `It should incremental migrate to version 2`() {
            val actual = session.incrementalMigrateDatabase()

            assertAll {
                assertThat(actual).isSuccess()
                assertThat(rootV1.fromRootResolveSnoozleDatabaseVersion()).textContentsAreEqualTo("2")
            }
        }

        @Test
        fun `When migrating segment with incorrect from, it should throw`() {
            val actual = session.migrateDatabase(null to 1)

            assertThat(actual).all {
                isFailure()
                    .messageContains("Migration does not match current version")
            }
        }
    }

    @Nested
    inner class MigrateSampleDatabaseVersion2To3 {
        @TempDir lateinit var rootV2: Path
        @TempDir lateinit var rootV3: Path

        lateinit var session: AdministrativeSession

        @BeforeEach
        fun before() {
            session = SampleDatabaseFixture
                .factory(
                    root = rootV2,
                    version = 2
                )
                .openAdministrativeSession()
                .getOrThrow()
            SampleDatabaseFixture.factory(
                root = rootV3,
                version = 3
            )
        }

        @Test
        fun `It should write upgraded database version`() {
            val actual = session.migrateDatabase(2 to 3)

            assertAll {
                assertThat(actual).isSuccess()
                assertThat(rootV2.fromRootResolveSnoozleDatabaseVersion()).textContentsAreEqualTo("3")
            }
        }

        @Test
        fun `It should add and remove properties on widgets`() {
            val actual = session.migrateDatabase(2 to 3)

            assertAll {
                assertThat(actual).isSuccess()
                val widgetOneActualJson = Widgets.tempFile(rootV2, 3, Widgets.One.id).readText()
                val widgetTwoActualJson = Widgets.tempFile(rootV2, 3, Widgets.Two.id).readText()
                val widgetOneExpectedJson = Widgets.tempFile(rootV3, 3, Widgets.One.id).readText()
                val widgetTwoExpectedJson = Widgets.tempFile(rootV3, 3, Widgets.Two.id).readText()
                JSONAssert.assertEquals(widgetOneExpectedJson, widgetOneActualJson, true)
                JSONAssert.assertEquals(widgetTwoExpectedJson, widgetTwoActualJson, true)
            }
        }


        @Test
        fun `It should auto migrate to version 3`() {
            val actual = session.autoMigrateDatabase()

            assertThat {
                assertThat(actual).isSuccess()
                assertThat(rootV2.fromRootResolveSnoozleDatabaseVersion()).textContentsAreEqualTo("3")
            }
        }

        @Test
        fun `It should fail to auto migrate when already on version 3`() {
            val session = SampleDatabase(rootV3.asAbsolute())
                .openAdministrativeSession()
                .getOrThrow()
            try {
                val actual = session.autoMigrateDatabase()

                assertThat(actual)
                    .isFailure()
                    .isInstanceOf(MigrationException::class)
                    .hasMessage("Cannot migrate because version on disk is not lower than latest database version")
            } finally {
                session.closeAndAssertSuccess()
            }
        }

        @Test
        fun `It should incremental migrate to version 3`() {
            val actual = session.incrementalMigrateDatabase()

            assertThat {
                assertThat(actual).isSuccess()
                assertThat(rootV2.fromRootResolveSnoozleDatabaseVersion()).textContentsAreEqualTo("3")
            }
        }

        @Test
        fun `When migrating segment with incorrect from, it should throw`() {
            val actual = session.migrateDatabase(1 to 2)

            assertThat(actual).all {
                isFailure()
                    .messageContains("Migration does not match current version")
            }
        }
    }
}

private fun Path.findAllAsRelative(): List<Path> {
    return Files.find(this, Int.MAX_VALUE, { _, _ -> true})
        .map { relativize(it) }
        .toList()
}

private fun Assert<Path>.textContentsAreEqualTo(expected: String, ignoreCase: Boolean = false) =
    transform { path -> path.readText() }
        .isEqualTo(other = expected, ignoreCase = ignoreCase)

private fun Path.fromRootResolveSnoozleDatabaseVersion(): Path {
    return resolve(".snoozle").resolve("database_version")
}