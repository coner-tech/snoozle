package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import java.util.*

data class EntityEvent<E : Entity<K>, K : Key>(val state: State, val id: UUID, val entity: E? = null) {
    enum class State {
        EXISTS,
        DELETED,
        OVERFLOW
    }
}