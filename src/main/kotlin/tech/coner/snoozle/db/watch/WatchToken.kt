package tech.coner.snoozle.db.watch

import kotlinx.coroutines.flow.Flow

interface WatchToken {
    val events: Flow<Event>

    suspend fun destroy()
}