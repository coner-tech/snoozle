package tech.coner.snoozle.db.watch

class TestWatchStore<SWT : StorableWatchToken, SWS : StorableWatchScope<SWT>> : WatchStore<SWT, SWS>() {
    val testScopes: Map<SWT, SWS>
        get() = scopes
    val testNextTokenId: Int
        get() = nextTokenId
    val testDestroyedTokenIdRanges: Set<ClosedRange<Int>>
        get() = destroyedTokenIdRanges
}