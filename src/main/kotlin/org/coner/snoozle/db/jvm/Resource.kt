package org.coner.snoozle.db.jvm

import de.helmbold.rxfilewatcher.PathObservables
import io.reactivex.Observable
import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.Resource
import org.coner.snoozle.util.uuid
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KProperty1

fun <E : Entity> Resource<E>.watchListing(vararg ids: Pair<KProperty1<E, UUID>, UUID>): Observable<EntityEvent<E>> {
    val file = File(root, path.findListing(*ids))
    return PathObservables.watchNonRecursive(file.toPath())
            .filter { path.isValidEntity((it.context() as Path).toFile()) }
            .map {
                val file = File(file, (it.context() as Path).toFile().name)
                val entity = if (file.exists() && file.length() > 0) {
                    reader.readValue<E>(file)
                } else {
                    null
                }
                EntityEvent(it, uuid(file.nameWithoutExtension), entity)
            }
}