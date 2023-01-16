package tech.coner.snoozle.db.watch

import kotlinx.coroutines.CoroutineScope
import tech.coner.snoozle.db.Key
import tech.coner.snoozle.db.entity.Entity
import kotlin.coroutines.CoroutineContext

class EntityWatchEngine<E : Entity<K>, K : Key>(
    override val coroutineContext: CoroutineContext,
    private val fileWatchEngine: FileWatchEngine
) : CoroutineScope {


}