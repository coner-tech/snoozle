package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.LiteralRecordDefinition

class EntityDefinition<E : Entity<K>, K : Key> : LiteralRecordDefinition<E, K>() {

    var key: (EntityKeyParser<E, K>.Context.() -> K)? = null

}
