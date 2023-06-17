package tech.coner.snoozle.db.entity

import tech.coner.snoozle.db.LiteralRecordDefinition
import kotlin.reflect.KClass

class EntityDefinition<K : tech.coner.snoozle.db.Key, E : Entity<K>>(
        keyClass: KClass<K>,
        entityClass: KClass<E>
) : LiteralRecordDefinition<K, E>(keyClass, entityClass)