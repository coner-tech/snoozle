package tech.coner.snoozle.db.watch

import kotlinx.coroutines.runBlocking

open class WatchStore<ID : Any, C : Any, SWT : StorableWatchToken<ID, C>, SWS : StorableWatchScope<ID, C, SWT>> {

    protected val scopes = mutableMapOf<SWT, SWS>()
    protected var nextTokenId = Int.MIN_VALUE
    protected val destroyedTokenIdRanges: MutableSet<ClosedRange<Int>> = mutableSetOf()

    fun create(
        tokenFactory: (id: Int) -> SWT,
        scopeFactory: suspend (SWT) -> SWS
    ): Pair<SWT, SWS> {
        val token = tokenFactory(
            chooseNextTokenId()
                ?: throw TokenCapacityLimitException("Too many tokens created and not destroyed")
        )
        val scope = runBlocking { scopeFactory(token) }
        scopes[token] = scope
        return token to scope
    }

    operator fun set(token: SWT, scope: SWS) {
        scopes[token] = scope
    }

    operator fun get(token: SWT): SWS {
        return scopes[token]
            ?: throw TokenNotFoundException("Token not found in store")
    }

    val allScopes: Collection<SWS>
        get() = scopes.values

    fun isEmpty() = scopes.isEmpty()

    private fun chooseNextTokenId(): Int? {
        val takeRange = destroyedTokenIdRanges.minByOrNull { it.start }
        return when {
            takeRange != null -> {
                destroyedTokenIdRanges.remove(takeRange)
                if (takeRange.start < takeRange.endInclusive) {
                    destroyedTokenIdRanges.add((takeRange.start + 1)..takeRange.endInclusive)
                }
                takeRange.start
            }

            nextTokenId < Int.MAX_VALUE -> nextTokenId++
            else -> null
        }
    }

    fun destroy(
        token: SWT,
        afterDestroyFn: suspend (SWS) -> Unit
    ) {
        val scope = scopes[token]
        if (scope == null || token.destroyed) {
            return // already destroyed, ignore
        }

        scopes.remove(token)
        runBlocking { afterDestroyFn.invoke(scope) }

        // record the scope id as destroyed
        val relevantRanges = destroyedTokenIdRanges.filter { candidate ->
            candidate.endInclusive + 1 == scope.token.id
                    || candidate.start - 1 == scope.token.id
        }
        if (relevantRanges.isEmpty()) {
            destroyedTokenIdRanges += scope.token.id..scope.token.id
        } else if (relevantRanges.size == 1) {
            val relevantRange = relevantRanges.single()
            destroyedTokenIdRanges.remove(relevantRange)
            if (relevantRange.endInclusive + 1 == scope.token.id) {
                // replace with appended range
                destroyedTokenIdRanges.add(relevantRange.start..scope.token.id)
            } else {
                // replace with preprended range
                destroyedTokenIdRanges.add(scope.token.id..relevantRange.endInclusive)
            }
        } else if (relevantRanges.size == 2) {
            val sorted = relevantRanges.sortedBy { it.start }
            relevantRanges.forEach { destroyedTokenIdRanges.remove(it) }
            destroyedTokenIdRanges.add(sorted[0].start..sorted[1].endInclusive)
        } else {
            // something went wrong
        }
    }

    fun destroyAll(afterDestroyFn: suspend (SWS) -> Unit) {
        ArrayList(scopes.keys)
            .forEach { token -> destroy(token, afterDestroyFn) }
        scopes.clear()
    }
}