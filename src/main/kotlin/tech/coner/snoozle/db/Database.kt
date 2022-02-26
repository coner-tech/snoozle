package tech.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import tech.coner.snoozle.db.session.SessionFactory
import tech.coner.snoozle.db.session.administrative.AdministrativeSession
import tech.coner.snoozle.db.session.data.DataSession
import tech.coner.snoozle.util.snoozleJacksonObjectMapper
import java.nio.file.Path

abstract class Database(
    protected val root: Path,
    private val objectMapper: ObjectMapper = snoozleJacksonObjectMapper(),
    sessionFactory: SessionFactory? = null
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
        return TypesRegistry(root, objectMapper)
            .apply(op)
    }

    fun openAdministrativeSession(): Result<AdministrativeSession> {
        return sessionFactory.createAdministrativeSession()
    }

    fun openDataSession(): Result<DataSession> {
        return sessionFactory.createDataSession()
    }

    protected fun registerMigrations(op: MigrationsRegistry.Builder.() -> Unit): MigrationsRegistry {
        return MigrationsRegistry.Builder(objectMapper)
            .apply(op)
            .build()
    }
}

