package tech.coner.snoozle.db.session.administrative

import tech.coner.snoozle.db.MigrationsRegistry
import tech.coner.snoozle.db.initialization.CannotInitializeException
import tech.coner.snoozle.db.initialization.FailedToInitializeException
import tech.coner.snoozle.db.metadata.MetadataRepository
import tech.coner.snoozle.db.migration.MigrationException
import tech.coner.snoozle.db.migration.Segment
import tech.coner.snoozle.db.session.Session
import java.nio.file.Path
import java.util.*

class AdministrativeSession(
    private val root: Path,
    private val version: Int,
    private val migrationsRegistry: MigrationsRegistry,
    metadataRepository: MetadataRepository,
    onClose: (sessionToClose: Session) -> Unit
) : Session(
    id = UUID.randomUUID(),
    metadataRepository = metadataRepository,
    onClose = onClose
) {

    fun initializeDatabase(): Result<Unit> = execute {
        if (metadataRepository.rootContainsAnythingOtherThanCurrentSessionMetadata(id)) {
            throw CannotInitializeException("Must initialize into empty folder")
        }
        try {
            metadataRepository.writeVersion(version)
        } catch (throwable: Throwable) {
            throw FailedToInitializeException("Attempted but failed to initialize", throwable)
        }
    }

    fun migrateDatabase(segment: Pair<Int?, Int>): Result<Unit> = execute {
        doMigrateDatabase(Segment(segment.first, segment.second))
    }

    fun migrateDatabase(from: Int?, to: Int): Result<Unit> = execute {
        doMigrateDatabase(Segment(from = from, to = to))
    }

    fun migrateDatabase(segment: Segment): Result<Unit> = execute {
        doMigrateDatabase(segment)
    }

    private fun doMigrateDatabase(segment: Segment) {
        val currentVersion = metadataRepository.readVersion()
        if (currentVersion != segment.from) {
            throw MigrationException("Migration does not match current version")
        }
        val migration = migrationsRegistry.migrations[segment]
            ?: throw MigrationException("Migration for segment not found")
        try {
            for (task in migration.tasks) {
                task.migrate(root)
            }
        } catch (t: Throwable) {
            throw MigrationException("Failed to execute migration. Suggest to restore from backup and fix your migration.")
        }
        metadataRepository.writeVersion(segment.to)
    }

    fun autoMigrateDatabase(): Result<Unit> = execute {
        val maximumVersion = version
        val originalCurrentVersion = metadataRepository.readVersion()
        if (maximumVersion <= (originalCurrentVersion ?: 0)) {
            throw MigrationException("Cannot migrate because version on disk is not lower than latest database version")
        }
        fun Int?.nextVersion() = when (this) {
            null -> 1
            else -> plus(1)
        }
        var currentVersion = originalCurrentVersion
        while (currentVersion != maximumVersion) {
            val nextSegment = Segment(
                from = currentVersion,
                to = currentVersion.nextVersion()
            )
            doMigrateDatabase(nextSegment)
            currentVersion = nextSegment.to
        }
    }

    fun incrementalMigrateDatabase(): Result<Unit> = execute {
        val currentVersion = metadataRepository.readVersion()
        doMigrateDatabase(
            segment = Segment(
                from = currentVersion,
                to = (currentVersion ?: 0) + 1
            )
        )
    }
}