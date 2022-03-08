package tech.coner.snoozle.db.session

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.coner.snoozle.db.MigrationsRegistry
import tech.coner.snoozle.db.TypesRegistry
import tech.coner.snoozle.db.blob.BlobIoException
import tech.coner.snoozle.db.entity.EntityIoException
import tech.coner.snoozle.db.metadata.*
import tech.coner.snoozle.db.session.administrative.AdministrativeSession
import tech.coner.snoozle.db.session.administrative.AdministrativeSessionException
import tech.coner.snoozle.db.session.data.DataSession
import tech.coner.snoozle.db.session.data.DataSessionException
import java.nio.file.Path

class SessionFactory(
    private val version: Int,
    private val root: Path,
    private val types: TypesRegistry,
    private val migrations: MigrationsRegistry,
    metadataRepository: MetadataRepository? = null
) {

    private val sessionMutex = Mutex()
    private var session: Session? = null
    private val metadataRepository: MetadataRepository = metadataRepository
        ?: MetadataRepository(
            root = root,
            databaseVersionResource = types.blobResources[DatabaseVersionBlob::class] as DatabaseVersionResource,
            sessionMetadataResource = types.entityResources[SessionMetadataEntity::class] as SessionMetadataResource
        )

    private fun enforceAbsoluteMinimumVersion() {
        check(version >= 1) { "Version must be greater than or equal to 1" }
    }

    private fun <S : Session> openSession(
        fn: () -> S,
        onWriteNewSessionMetadataFailed: (Throwable) -> Throwable
    ): Result<S> = runBlocking {
        try {
            enforceAbsoluteMinimumVersion()
            sessionMutex.withLock {
                if (session != null) {
                    throw SessionException.AlreadyExists()
                }
                fn()
                    .also {
                        session = it
                        try {
                            val sessionMetaDataEntity = it.toSessionMetadataEntity()
                            metadataRepository.writeNewSessionMetadata(sessionMetaDataEntity)
                        } catch (t: Throwable) {
                            throw onWriteNewSessionMetadataFailed(t)
                        }
                    }
                    .let { Result.success(it) }
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun createAdministrativeSession(): Result<AdministrativeSession> = openSession(
        fn = {
            val sessions = metadataRepository.listSessions()
            if (sessions.isNotEmpty()) {
                throw AdministrativeSessionException.ConcurrentSessionsNotPermitted(sessions)
            }
            AdministrativeSession(
                metadataRepository = metadataRepository,
                onClose = ::onSessionClosed,
                root = root,
                version = version,
                migrationsRegistry = migrations
            )
        },
        onWriteNewSessionMetadataFailed = { t -> when (t) {
            is EntityIoException.WriteFailure -> AdministrativeSessionException.MetadataWriteFailure(t)
            else -> AdministrativeSessionException.Unknown(t)
        } }
    )

    fun createDataSession(): Result<DataSession> = openSession(
        fn = {
            val versionOnDisk = try {
                metadataRepository.readVersion()
            } catch (me: MetadataException) {
                val cause = me.cause
                if (cause is BlobIoException && cause.reason == BlobIoException.Reason.ReadFailure) {
                    throw DataSessionException.VersionReadFailure()
                } else {
                    throw DataSessionException.Unknown(me)
                }
            } catch (t: Throwable) {
                throw DataSessionException.Unknown(t)
            }
            when {
                versionOnDisk == null -> throw DataSessionException.VersionUndefined()
                versionOnDisk != version -> throw DataSessionException.VersionMismatch(
                    requiredVersion = version,
                    actualVersion = versionOnDisk
                )
            }
            DataSession(
                types = types,
                metadataRepository = metadataRepository,
                onClose = ::onSessionClosed
            )
        },
        onWriteNewSessionMetadataFailed = { t -> when (t) {
            is EntityIoException.WriteFailure -> DataSessionException.MetadataWriteFailure(t)
            else -> DataSessionException.Unknown(t)
        } }
    )

    private fun onSessionClosed(sessionToClose: Session): Unit = runBlocking {
        sessionMutex.withLock {
            if (sessionToClose.closed) {
                throw SessionException.AlreadyClosed()
            }
            metadataRepository.deleteSessionMetadata(sessionToClose.toSessionMetadataKey())
            session = null
        }
    }
}