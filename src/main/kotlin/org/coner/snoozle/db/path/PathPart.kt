package org.coner.snoozle.db.path

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.Record
import org.coner.snoozle.db.entity.*
import org.coner.snoozle.util.hasUuidPattern
import java.io.File
import java.util.*
import java.util.regex.Pattern

sealed class PathPart<R : Record<K>, K : Key, P> {

    abstract fun pathPartFromKey(key: K): P?
    abstract fun keyValueFromPathPart(pathPart: String): P?
    abstract val regex: Pattern

    class StringValue<R : Record<K>, K : Key>(
            private val value: String
    ) : PathPart<R, K, String>(), StaticExtractor<R> {
        override fun pathPartFromKey(key: K) = value
        override fun keyValueFromPathPart(pathPart: String) = value
        override val regex: Pattern = Pattern.compile(Pattern.quote(value))
    }

    class DirectorySeparator<R : Record<K>, K : Key> : PathPart<R, K, String>(), StaticExtractor<R> {
        override fun pathPartFromKey(key: K): String = File.separator
        override fun keyValueFromPathPart(pathPart: String): String = File.separator
        override val regex: Pattern = regexPattern

        companion object {
            private val regexPattern by lazy { Pattern.compile(Pattern.quote(File.separator)) }
        }
    }

    interface StaticExtractor<R>
    interface VariableExtractor<R>

    class UuidVariable<R : Record<K>, K : Key>(
            private val extractor: (K) -> UUID
    ) : PathPart<R, K, UUID>(), VariableExtractor<R> {
        override fun pathPartFromKey(key: K) = extractor(key)
        override fun keyValueFromPathPart(pathPart: String): UUID = UUID.fromString(pathPart)
        override val regex = hasUuidPattern
    }

    class StringVariable<R : Record<K>, K : Key>(
            private val extractor: (K) -> String
    ) : PathPart<R, K, String>(), VariableExtractor<R> {
        override fun pathPartFromKey(key: K): String = extractor(key)
        override fun keyValueFromPathPart(pathPart: String) = pathPart
        override val regex = alphanumericWithHyphensAndUnderscores

        companion object {
            val alphanumericWithHyphensAndUnderscores: Pattern by lazy {
                Pattern.compile("[\\w-]*")
            }
        }
    }

    class VersionArgumentVariable<
            R : VersionedEntityContainer<E, K>,
            K : VersionedEntityContainerKey<EK>,
            E : VersionedEntity<EK>,
            EK : Key
    > : PathPart<R, K, VersionArgument>(), VariableExtractor<R> {
        override fun pathPartFromKey(key: K): VersionArgument? {
            TODO("Not yet implemented")
        }

        override fun keyValueFromPathPart(pathPart: String): VersionArgument? {
            TODO("Not yet implemented")
        }

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