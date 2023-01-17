package tech.coner.snoozle.db.watch

interface StorableWatchToken<ID : Any, C : Any> : WatchToken<ID, C> {
    val id: Int
    var destroyed: Boolean
}