package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.LiteralRecordDefinition

class EntityDefinition<E : Entity<K>, K : Key> : LiteralRecordDefinition<E, K>() {

    var entityKeyParser: EntityKeyParser<K>? = null
        private set

    fun entityKeyParser(fn: EntityKeyParserContext<E, K>.() -> K) {
        entityKeyParser = EntityKeyParser(fn)
    }

}
