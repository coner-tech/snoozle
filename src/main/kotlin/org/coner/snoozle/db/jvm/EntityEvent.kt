package org.coner.snoozle.db.jvm

import org.coner.snoozle.db.Entity
import java.nio.file.WatchEvent

data class EntityEvent<E : Entity>(val watchEvent: WatchEvent<*>, val entity: E? = null)