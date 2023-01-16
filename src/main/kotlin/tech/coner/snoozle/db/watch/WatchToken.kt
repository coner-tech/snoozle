package tech.coner.snoozle.db.watch

import kotlinx.coroutines.flow.Flow

interface WatchToken<T> {
    val events: Flow<T>
}