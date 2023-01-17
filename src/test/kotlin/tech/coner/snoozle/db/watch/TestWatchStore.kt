package tech.coner.snoozle.db.watch

class TestWatchStore<ID : Any, C : Any, SWT : StorableWatchToken<ID, C>, SWS : StorableWatchScope<ID, C, SWT>> : WatchStore<ID, C, SWT, SWS>() {
    val testScopes: Map<SWT, SWS>
        get() = scopes
    val testNextTokenId: Int
        get() = nextTokenId
    val testDestroyedTokenIdRanges: Set<ClosedRange<Int>>
        get() = destroyedTokenIdRanges
}