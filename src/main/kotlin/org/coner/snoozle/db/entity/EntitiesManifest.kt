package org.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.db.path.Pathfinder
import org.coner.snoozle.db.versioning.EntityVersioningStrategy
import java.nio.file.Path
import kotlin.reflect.KClass

class EntitiesManifest(
        val root: Path,
        op: EntitiesManifest.(Map<KClass<*>, EntityResource<*>>) -> Unit,
        val objectMapper: ObjectMapper
) {
    val entityResources = mutableMapOf<KClass<*>, EntityResource<*>>()

    init {
        this.op(entityResources)
    }

    inline fun <reified E : Entity> entity(op: EntityDefinition<E>.() -> Unit) {
        val entityDefinition = EntityDefinition<E>().apply(op)
        entityResources[E::class] = EntityResource(
                root = root,
                entityDefinition = entityDefinition,
                objectMapper = objectMapper,
                path = Pathfinder(entityDefinition.path),
                entityIoDelegate = EntityIoDelegate(
                        objectMapper = objectMapper,
                        reader = objectMapper.readerFor(E::class.java),
                        writer = objectMapper.writerFor(E::class.java)
                ),
                automaticEntityVersionIoDelegate = when (entityDefinition.versioning) {
                    EntityVersioningStrategy.AutomaticInternalVersioning -> AutomaticEntityVersionIoDelegate(
                            reader = objectMapper.readerFor(E::class.java),
                            entityDefinition = entityDefinition
                    )
                    else -> null
                }
        )
    }
}