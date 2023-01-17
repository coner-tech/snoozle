package tech.coner.snoozle.db.watch

import kotlinx.coroutines.flow.Flow

interface WatchToken<ID : Any, C : Any> {
    val events: Flow<Event<ID, C>>

    suspend fun destroy()
}