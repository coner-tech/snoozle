package tech.coner.snoozle.db.entity

import tech.coner.snoozle.db.Key
import java.util.*

data class EntityEvent<K : tech.coner.snoozle.db.Key, E : Entity<K>>(val state: State, val key: K, val entity: E? = null) {
    enum class State {
        EXISTS,
        DELETED,
        OVERFLOW
    }
}