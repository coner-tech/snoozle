package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.closeAndAssertSuccess
import tech.coner.snoozle.db.initialization.CannotInitializeException
import tech.coner.snoozle.db.initialization.FailedToInitializeException
import tech.coner.snoozle.db.sample.SampleDatabase
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.session.administrative.AdministrativeSession
import tech.coner.snoozle.util.resolve
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class InitializationIntegrationTest {

    @TempDir lateinit var root: Path

    lateinit var session: AdministrativeSession

    @BeforeEach
    fun before() {
        session = SampleDatabase(root)
            .openAdministrativeSession()
            .getOrThrow()
    }

    @AfterEach
    fun after() {
        // one test marks a directory not writable which causes
        Files.find(root, 3, { _, attrs -> attrs.isDirectory })
            .forEach { it.toFile().setWritable(true) }
        session.closeAndAssertSuccess()
    }

    @Test
    fun `Test prerequisite that root contains only current session metadata`() {
        val actual = Files.find(root, 3, { _, attrs -> attrs.isRegularFile}).toList()

        val expected = listOf(root.resolve(".snoozle", "sessions", "${session.id}.json"))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `It should initialize folder as database`() {
        val actual = session.initializeDatabase()

        assertAll {
            assertThat(actual).isSuccess()
            val actualDatabaseVersionFile = root.resolve(".snoozle", "database_version")
            actualDatabaseVersionFile.exists()
            val actualDatabaseVersionFileContents = actualDatabaseVersionFile.readText()
            assertThat(actualDatabaseVersionFileContents).isEqualTo("${SampleDatabaseFixture.VERSION_HIGHEST}")
        }
    }

    @Test
    fun `It should fail to initialize database if folder contains anything`(@TempDir root: Path) {
        val sampleDatabase = SampleDatabaseFixture.factory(root, SampleDatabaseFixture.VERSION_HIGHEST)
        val session = sampleDatabase.openAdministrativeSession().getOrThrow()

        val actual = session.initializeDatabase()

        assertThat(actual).all {
            isFailure()
                .isInstanceOf(CannotInitializeException::class)
        }
    }

    @Test
    fun `It should fail to initialize if folder not writable`() {
        Files.find(root, 3, { _, attrs -> attrs.isDirectory })
            .forEach { it.toFile().setWritable(false) }

        val actual = session.initializeDatabase()

        assertThat(actual).all {
            isFailure()
                .isInstanceOf(FailedToInitializeException::class)
        }
    }
}