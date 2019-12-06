package org.coner.snoozle.db.path

import java.io.File
import java.util.*

sealed class PathPart<R> {

    abstract fun extractQueryArgument(arg: Any?): String

    class StringPathPart<R>(val part: String) : PathPart<R>() {
        override fun extractQueryArgument(arg: Any?) = part
    }

    class DirectorySeparatorPathPart<R> : PathPart<R>() {
        override fun extractQueryArgument(arg: Any?) = File.separator
    }

    interface VariablePathPart<E>

    class UuidPathPart<R>(
            val recordExtractor: (R) -> UUID
    ) : PathPart<R>(), VariablePathPart<R> {
        override fun extractQueryArgument(arg: Any?) = (arg as UUID).toString()

    }
}