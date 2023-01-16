package tech.coner.snoozle.db.watch

interface StorableWatchScope<SWT : StorableWatchToken> {
    val token: SWT
}