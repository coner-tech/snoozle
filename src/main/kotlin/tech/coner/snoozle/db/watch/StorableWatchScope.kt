package tech.coner.snoozle.db.watch

interface StorableWatchScope<ID : Any, C : Any, SWT : StorableWatchToken<ID, C>> {
    val token: SWT
}