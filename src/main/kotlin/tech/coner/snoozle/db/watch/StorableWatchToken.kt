package tech.coner.snoozle.db.watch

interface StorableWatchToken<E> : WatchToken<E> {
    val id: Int
    var destroyed: Boolean
}