package tech.coner.snoozle.db

import assertk.Assert
import assertk.assertions.isInstanceOf
import assertk.assertions.prop

fun Assert<FileWatchEngine.Event>.isRecordExistsInstance() = isInstanceOf(FileWatchEngine.Event.File.Exists::class)
fun Assert<FileWatchEngine.Event>.isRecordDoesNotExistsInstance() = isInstanceOf(FileWatchEngine.Event.File.DoesNotExist::class)
fun Assert<FileWatchEngine.Event.File>.record() = prop(FileWatchEngine.Event.File::file)
fun Assert<FileWatchEngine.Event>.isOverflowInstance() = isInstanceOf(FileWatchEngine.Event.Overflow::class)