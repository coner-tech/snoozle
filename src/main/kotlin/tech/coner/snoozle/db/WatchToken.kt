package tech.coner.snoozle.db

import kotlinx.coroutines.flow.Flow

interface WatchToken<T> {
    val events: Flow<T>
}