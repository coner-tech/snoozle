package tech.coner.snoozle.db.watch

interface StorableWatchScope<SWT : StorableWatchToken<T>, T> {
    val token: SWT
}