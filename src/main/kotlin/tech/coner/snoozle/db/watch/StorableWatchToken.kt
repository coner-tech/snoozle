package tech.coner.snoozle.db.watch

interface StorableWatchToken : WatchToken {
    val id: Int
    var destroyed: Boolean
}