package org.coner.snoozle.db.path

import java.io.File
import java.util.*

sealed class PathPart<R> {

    abstract fun extractQueryArgument(arg: Any?): String
    abstract fun forRecord(record: R): String

    class StringPathPart<R>(val part: String) : PathPart<R>() {
        override fun extractQueryArgument(arg: Any?) = part
        override fun forRecord(record: R) = part
    }

    class DirectorySeparatorPathPart<R> : PathPart<R>() {
        override fun extractQueryArgument(arg: Any?) = File.separator
        override fun forRecord(record: R) = File.separator
    }

    interface VariablePathPart<E>

    class UuidPathPart<R>(
            private val recordExtractor: (R) -> UUID
    ) : PathPart<R>(), VariablePathPart<R> {
        override fun extractQueryArgument(arg: Any?) = (arg as UUID).toString()
        override fun forRecord(record: R) = recordExtractor(record).toString()
    }
}