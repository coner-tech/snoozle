package org.coner.snoozle.db

import java.util.*

data class EntityEvent<E : Entity>(val state: State, val id: UUID, val entity: E? = null) {
    enum class State {
        EXISTS,
        DELETED,
        OVERFLOW
    }
}