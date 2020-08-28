package org.coner.snoozle.db

import org.coner.snoozle.util.hasUuidPattern
import java.io.File
import java.util.*
import java.util.regex.Pattern

sealed class PathPart<K : Key, R : Record<*>, P> {

    abstract fun pathPartFromKey(key: K): P?
    abstract val regex: Pattern

    class StringValue<K : Key, R : Record<K>>(
            private val value: String
    ) : PathPart<K, R, String>(), StaticExtractor<R> {
        override fun pathPartFromKey(key: K) = value
        override val regex: Pattern = Pattern.compile(Pattern.quote(value))
    }

    class DirectorySeparator<K : Key, R : Record<K>> : PathPart<K, R, String>(), StaticExtractor<R> {
        override fun pathPartFromKey(key: K): String = File.separator
        override val regex: Pattern = regexPattern

        companion object {
            private val regexPattern by lazy { Pattern.compile(Pattern.quote(File.separator)) }
        }
    }

    interface StaticExtractor<R>
    interface VariableExtractor<R, P> {
        fun keyValueFromPathPart(part: String): P
    }

    class UuidVariable<K : Key, R : Record<K>>(
            private val extractor: K.() -> UUID
    ) : PathPart<K, R, UUID>(), VariableExtractor<R, UUID> {
        override fun pathPartFromKey(key: K) = extractor(key)
        override fun keyValueFromPathPart(pathPart: String): UUID = UUID.fromString(pathPart)
        override val regex: Pattern = hasUuidPattern
    }

    class StringVariable<K : Key, R : Record<K>>(
            private val extractor: K.() -> String
    ) : PathPart<K, R, String>(), VariableExtractor<R, String> {
        override fun pathPartFromKey(key: K): String = extractor(key)
        override fun keyValueFromPathPart(pathPart: String) = pathPart
        override val regex = alphanumericWithHyphensAndUnderscores

        companion object {
            val alphanumericWithHyphensAndUnderscores: Pattern by lazy {
                Pattern.compile("[\\w-]*")
            }
        }
    }
}