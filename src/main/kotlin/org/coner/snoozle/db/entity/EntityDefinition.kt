package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.LiteralRecordDefinition
import kotlin.reflect.KClass

class EntityDefinition<K : Key, E : Entity<K>>(
        keyClass: KClass<K>,
        entityClass: KClass<E>
) : LiteralRecordDefinition<K, E>(keyClass, entityClass) {

    var keyFromEntity: (E.() -> K)? = null

}
