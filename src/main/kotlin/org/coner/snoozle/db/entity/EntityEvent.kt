package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import java.util.*

data class EntityEvent<K : Key, E : Entity<K>>(val state: State, val key: K, val entity: E? = null) {
    enum class State {
        EXISTS,
        DELETED,
        OVERFLOW
    }
}