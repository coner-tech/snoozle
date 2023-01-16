package tech.coner.snoozle.db.watch

import kotlinx.coroutines.flow.Flow

interface WatchToken<E> {
    val events: Flow<E>

    suspend fun destroy()
}