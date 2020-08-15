package org.coner.snoozle.db.path

import org.coner.snoozle.db.entity.VersionArgument
import org.coner.snoozle.db.entity.VersionedEntityContainer
import org.coner.snoozle.util.hasUuidPattern
import java.io.File
import java.util.*
import java.util.regex.Pattern

sealed class PathPart<R> {

    abstract fun extractQueryArgument(arg: Any?): String
    abstract fun produceQueryArgument(pathPart: String): Any?
    abstract fun forRecord(record: R): String
    abstract val regex: Pattern

    class StringValue<R>(val value: String) : PathPart<R>(), StaticExtractor<R> {

        override fun extractQueryArgument(arg: Any?) = value
        override fun produceQueryArgument(pathPart: String) = null
        override fun forRecord(record: R) = value
        override val regex = Pattern.compile(value)
    }

    class DirectorySeparator<R> : PathPart<R>(), StaticExtractor<R> {
        private val regexPattern by lazy { Pattern.compile(File.separator) }

        override fun extractQueryArgument(arg: Any?) = File.separator
        override fun produceQueryArgument(pathPart: String) = null
        override fun forRecord(record: R) = File.separator
        override val regex = regexPattern
    }

    interface StaticExtractor<R>
    interface VariableExtractor<R>

    class UuidVariable<R>(
            private val recordExtractor: R.() -> UUID
    ) : PathPart<R>(), VariableExtractor<R> {
        override fun extractQueryArgument(arg: Any?) = (arg as UUID).toString()
        override fun produceQueryArgument(pathPart: String) = UUID.fromString(pathPart)
        override fun forRecord(record: R) = recordExtractor(record).toString()
        override val regex = hasUuidPattern
    }

    class StringVariable<R>(
            private val recordExtractor: R.() -> String
    ) : PathPart<R>(), VariableExtractor<R> {
        override fun extractQueryArgument(arg: Any?) = arg as String
        override fun produceQueryArgument(pathPart: String) = pathPart
        override fun forRecord(record: R) = recordExtractor(record)
        override val regex = alphanumericWithHyphensAndUnderscores

        companion object {
            val alphanumericWithHyphensAndUnderscores: Pattern by lazy {
                Pattern.compile("[\\w-]*")
            }
        }
    }

    class VersionArgumentVariable<R>(
    ) : PathPart<R>(), VariableExtractor<R> {
        override fun extractQueryArgument(arg: Any?): String {
            return (arg as VersionArgument).value
        }
        override fun produceQueryArgument(pathPart: String) = when {
            pathPart == VersionArgument.Auto.value -> VersionArgument.Auto
            positiveInteger.matcher(pathPart).matches() -> VersionArgument.Manual(pathPart.toInt())
            else -> throw IllegalArgumentException("Invalid pathPart segment: $pathPart")
        }
        override fun forRecord(record: R) = (record as VersionedEntityContainer<*, *>).version.toString()
        override val regex = positiveInteger

        companion object {
            val positiveInteger: Pattern by lazy {
                Pattern.compile("[\\d]{1,10}")
            }
        }
    }
}