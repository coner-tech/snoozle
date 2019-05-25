package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Observable
import org.coner.snoozle.util.snoozleJacksonObjectMapper
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KProperty1

abstract class Database(
        protected val root: Path,
        protected val objectMapper: ObjectMapper = snoozleJacksonObjectMapper()
) {

    protected abstract val entities: List<EntityDefinition<*>>
    val resources by lazy {
        entities.map {

            it.kClass to Resource(
                    root = root,
                    entityDefinition = it,
                    objectMapper = objectMapper
            )
        }.toMap()
    }

    inline fun <reified E : Entity> get(vararg ids: Pair<KProperty1<E, UUID>, UUID>): E {
        return findResource<E>().get(*ids)
    }

    inline fun <reified E : Entity> getWholeRecord(vararg ids: Pair<KProperty1<E, UUID>, UUID>): WholeRecord<E> {
        return findResource<E>().getWholeRecord(*ids)
    }

    inline fun <reified E : Entity> put(entity: E) {
        findResource<E>().put(entity)
    }

    inline fun <reified E : Entity> remove(entity: E) {
        findResource<E>().delete(entity)
    }

    inline fun <reified E : Entity> list(vararg ids: Pair<KProperty1<E, UUID>, UUID>): List<E> {
        return findResource<E>().list(*ids)
    }

    inline fun <reified E : Entity> entityDefinition(): EntityDefinition<E> {
        return EntityDefinition(E::class)
    }

    inline fun <reified E : Entity> watchListing(vararg ids: Pair<KProperty1<E, UUID>, UUID>): Observable<EntityEvent<E>> {
        val resource: Resource<E> = findResource()
        return resource.watchListing(*ids)
    }

    inline fun <reified E : Entity> findResource(): Resource<E> {
        return (resources[E::class] ?: throw IllegalArgumentException("No resource for ${E::class.qualifiedName}")) as Resource<E>
    }
}

