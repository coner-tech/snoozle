package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.LiteralRecordDefinition
import org.coner.snoozle.db.path.PathPart

class EntityDefinition<E : Entity<K>, K : Key> : LiteralRecordDefinition<E, K>() {

    var path: List<PathPart<E, K, *>> = mutableListOf()
    var key: (EntityKeyParser<E, K>.Context.() -> K)? = null

}
