package org.coner.snoozle.db.path

import org.coner.snoozle.db.entity.DiscreteVersionArgument
import org.coner.snoozle.db.entity.DiscreteVersionedEntityContainer
import org.coner.snoozle.util.hasUuidPattern
import java.io.File
import java.util.*
import java.util.regex.Pattern

sealed class PathPart<R> {

    abstract fun extractQueryArgument(arg: Any?): String
    abstract fun forRecord(record: R): String
    abstract val regex: Pattern

    class StringPathPart<R>(val part: String) : PathPart<R>() {

        override fun extractQueryArgument(arg: Any?) = part
        override fun forRecord(record: R) = part
        override val regex = Pattern.compile(part)
    }

    class DirectorySeparatorPathPart<R> : PathPart<R>() {
        private val regexPattern by lazy { Pattern.compile(File.separator) }

        override fun extractQueryArgument(arg: Any?) = File.separator
        override fun forRecord(record: R) = File.separator
        override val regex = regexPattern
    }

    interface VariablePathPart<R>

    class UuidVariablePathPart<R>(
            private val recordExtractor: (R) -> UUID
    ) : PathPart<R>(), VariablePathPart<R> {
        override fun extractQueryArgument(arg: Any?) = (arg as UUID).toString()
        override fun forRecord(record: R) = recordExtractor(record).toString()
        override val regex = hasUuidPattern
    }

    class StringVariablePathPart<R>(
            private val recordExtractor: R.() -> String
    ) : PathPart<R>(), VariablePathPart<R> {
        override fun extractQueryArgument(arg: Any?) = arg as String
        override fun forRecord(record: R) = recordExtractor(record)
        override val regex = alphanumericWithHyphensAndUnderscores

        companion object {
            val alphanumericWithHyphensAndUnderscores: Pattern by lazy {
                Pattern.compile("[\\w-]*")
            }
        }
    }

    class DiscreteVersionArgumentPathPart<R>(
    ) : PathPart<R>(), VariablePathPart<R> {
        override fun extractQueryArgument(arg: Any?): String {
            return (arg as DiscreteVersionArgument).value
        }

        override fun forRecord(record: R) = (record as DiscreteVersionedEntityContainer<*>).version.toString()

        override val regex = positiveInteger

        companion object {
            val positiveInteger: Pattern by lazy {
                Pattern.compile("[\\d]{1,10}")
            }
        }
    }
}