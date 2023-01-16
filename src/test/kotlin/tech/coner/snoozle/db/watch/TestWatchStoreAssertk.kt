package tech.coner.snoozle.db.watch

import assertk.Assert
import assertk.assertions.prop

fun Assert<TestWatchStore<*, *>>.nextTokenId() = prop("nextTokenId") { it.testNextTokenId }
fun Assert<TestWatchStore<*, *>>.destroyedTokenIdRanges() = prop("destroyedTokendRanges") { it.testDestroyedTokenIdRanges }
fun Assert<TestWatchStore<*, *>>.isEmpty() = prop(WatchStore<*, *>::isEmpty)
fun <SWT : StorableWatchToken, SWS : StorableWatchScope<SWT>> Assert<TestWatchStore<SWT, SWS>>.scopes() = prop("scopes") { it.testScopes }