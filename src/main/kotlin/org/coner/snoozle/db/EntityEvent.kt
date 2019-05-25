package org.coner.snoozle.db

import java.nio.file.WatchEvent
import java.util.*

data class EntityEvent<E : Entity>(val watchEvent: WatchEvent<*>, val id: UUID, val entity: E? = null)