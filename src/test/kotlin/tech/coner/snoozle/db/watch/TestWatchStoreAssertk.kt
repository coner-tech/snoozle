package tech.coner.snoozle.db.watch

import assertk.Assert
import assertk.assertions.prop

fun Assert<TestWatchStore<*, *, *, *>>.nextTokenId() = prop("nextTokenId") { it.testNextTokenId }
fun Assert<TestWatchStore<*, *, *, *>>.destroyedTokenIdRanges() = prop("destroyedTokendRanges") { it.testDestroyedTokenIdRanges }
fun Assert<TestWatchStore<*, *, *, *>>.isEmpty() = prop(WatchStore<*, *, *, *>::isEmpty)
fun <ID : Any, C : Any, SWT : StorableWatchToken<ID, C>, SWS : StorableWatchScope<ID, C, SWT>> Assert<TestWatchStore<ID, C, SWT, SWS>>.scopes() = prop("scopes") { it.testScopes }