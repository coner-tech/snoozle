package tech.coner.snoozle.db

import assertk.Assert
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotInstanceOf
import assertk.assertions.prop

fun Assert<WatchEngine.Event>.isRecordExistsInstance() = isInstanceOf(WatchEngine.Event.Record.Exists::class)
fun Assert<WatchEngine.Event>.isRecordDoesNotExistsInstance() = isNotInstanceOf(WatchEngine.Event.Record.DoesNotExist::class)
fun Assert<WatchEngine.Event.Record>.record() = prop(WatchEngine.Event.Record::record)
fun Assert<WatchEngine.Event>.isOverflowInstance() = isInstanceOf(WatchEngine.Event.Overflow::class)