package org.coner.snoozle.db.jvm

import io.reactivex.Observable
import org.coner.snoozle.db.Database
import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.Resource
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

inline fun <reified E: Entity> Database.watchListing(vararg ids: Pair<KProperty1<E, UUID>, UUID>): Observable<EntityEvent<E>> {
    val resource: Resource<E> = resources[E::class as KClass<Entity>]!! as Resource<E>
    return resource.watchListing(*ids)
}