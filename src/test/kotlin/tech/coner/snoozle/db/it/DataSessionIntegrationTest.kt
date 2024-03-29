package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.skyscreamer.jsonassert.JSONAssert
import tech.coner.snoozle.db.metadata.SessionMetadataEntity
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.session.data.DataSession
import tech.coner.snoozle.db.session.data.DataSessionException
import tech.coner.snoozle.util.doesNotExist
import tech.coner.snoozle.util.resolve
import java.net.InetAddress
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText

class DataSessionIntegrationTest {

    @TempDir lateinit var root: Path

    @Test
    fun `When version not available on disk, it should fail to open`() {
        val database = SampleDatabaseFixture.factory(root, SampleDatabaseFixture.VERSION_HIGHEST)
        root.resolve(".snoozle", "database_version")
            .deleteExisting()

        val actual = database.openDataSession()

        assertThat(actual)
            .isFailure()
            .isInstanceOf(DataSessionException.VersionUndefined::class)
    }

    @Test
    fun `When it cannot read version on disk, it should fail to open`() {
        val database = SampleDatabaseFixture.factory(root, SampleDatabaseFixture.VERSION_HIGHEST)
        root.resolve(".snoozle", "database_version")
            .also {
                it.toFile().setReadable(false)
            }

        val actual = database.openDataSession()

        assertThat(actual)
            .isFailure()
            .isInstanceOf(DataSessionException.VersionReadFailure::class)
    }

    @Test
    fun `When the database version on disk doesn't match defined version, it should fail to open`() {
        val actualVersion = SampleDatabaseFixture.VERSION_HIGHEST - 1
        val database = SampleDatabaseFixture.factory(root, actualVersion)

        val actual = database.openDataSession()

        assertThat(actual)
            .isFailure()
            .isInstanceOf(DataSessionException.VersionMismatch::class).all {
                prop(DataSessionException.VersionMismatch::requiredVersion).isEqualTo(SampleDatabaseFixture.VERSION_HIGHEST)
                prop(DataSessionException.VersionMismatch::actualVersion).isEqualTo(actualVersion)
            }
    }

    @Test
    fun `It should open an established and proper database`() {
        val database = SampleDatabaseFixture.factory(root, SampleDatabaseFixture.VERSION_HIGHEST)

        val actual = database.openDataSession()

        assertThat(actual).isSuccess()
    }

    @Test
    fun `When it opens, it should write its metadata`() {
        val database = SampleDatabaseFixture.factory(root, SampleDatabaseFixture.VERSION_HIGHEST)

        val session = database.openDataSession().getOrThrow()

        val sessionPath = session.path
        assertAll {
            assertThat(sessionPath).all {
                exists()
                val expectedJson = """
                    {
                        "id": "${session.id}",
                        "host": "${InetAddress.getLocalHost().canonicalHostName}",
                        "processId": ${ProcessHandle.current().pid()},
                        "type": "${SessionMetadataEntity.Type.DATA}"
                    }
                """.trimIndent()
                JSONAssert.assertEquals(expectedJson, sessionPath.readText(), false)
            }
        }
    }

    @Test
    fun `When it closes, it should clear its metadata`() {
        val database = SampleDatabaseFixture.factory(root, SampleDatabaseFixture.VERSION_HIGHEST)
        val session = database.openDataSession().getOrThrow()

        val actual = session.close()

        assertAll {
            assertThat(actual, "session closed").isSuccess()
            val sessionPath = session.path
            assertThat(sessionPath).doesNotExist()
        }
    }

    @Test
    fun `When the session metadata cannot be written, it should fail to open`() {
        val database = SampleDatabaseFixture.factory(root, SampleDatabaseFixture.VERSION_HIGHEST)
        root.resolve(".snoozle", "sessions")
            .also { it.createDirectories() }
            .also { it.toFile().setWritable(false) }

        val actual = database.openDataSession()

        assertThat(actual)
            .isFailure()
            .isInstanceOf(DataSessionException.MetadataWriteFailure::class)
    }


    private val DataSession.path: Path
        get() = root.resolve(".snoozle", "sessions", "$id.json")
}
