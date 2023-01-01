package tech.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import tech.coner.snoozle.db.session.SessionFactory
import tech.coner.snoozle.db.session.administrative.AdministrativeSession
import tech.coner.snoozle.db.session.data.DataSession
import tech.coner.snoozle.util.snoozleJacksonObjectMapper

abstract class Database(
    protected val root: AbsolutePath,
    private val objectMapper: ObjectMapper = snoozleJacksonObjectMapper(),
    sessionFactory: SessionFactory? = null,
    private val fileWatchEngine: FileWatchEngine = FileWatchEngine(
        coroutineContext = Dispatchers.IO + Job(),
        root = root
    )
) {
    protected abstract val version: Int
    protected abstract val types: TypesRegistry
    protected abstract val migrations: MigrationsRegistry
    private val sessionFactory by lazy {
        sessionFactory
            ?: SessionFactory(
                version = version,
                root = root,
                types = types,
                migrations = migrations
            )
    }

    protected fun registerTypes(op: TypesRegistry.() -> Unit): TypesRegistry {
        return TypesRegistry(root, fileWatchEngine, objectMapper)
            .apply(op)
    }

    fun openAdministrativeSession(): Result<AdministrativeSession> {
        return sessionFactory.createAdministrativeSession()
    }

    fun useAdministrativeSession(fn: (administrativeSession: AdministrativeSession) -> Unit) {
        openAdministrativeSession()
            .getOrThrow()
            .use(fn)
    }

    fun openDataSession(): Result<DataSession> {
        return sessionFactory.createDataSession()
    }

    fun useDataSession(fn: (dataSession: DataSession) -> Unit) {
        openDataSession()
            .getOrThrow()
            .use(fn)
    }

    protected fun registerMigrations(op: MigrationsRegistry.Builder.() -> Unit): MigrationsRegistry {
        return MigrationsRegistry.Builder(objectMapper)
            .apply(op)
            .build()
    }
}

