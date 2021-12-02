package tech.coner.snoozle.db

import tech.coner.snoozle.util.hasUuidPattern
import java.io.File
import java.util.*
import java.util.regex.Pattern

sealed class PathPart<K : Key, R : Record<*>, P> {

    abstract val regex: Pattern

    class StringValue<K : Key, R : Record<K>>(
            override val value: String
    ) : PathPart<K, R, String>(), StaticExtractor<R> {
        override val regex: Pattern = Pattern.compile(Pattern.quote(value))
    }

    class DirectorySeparator<K : Key, R : Record<K>> : PathPart<K, R, String>(), StaticExtractor<R> {
        override val value: String = File.separator
        override val regex: Pattern = regexPattern

        companion object {
            private val regexPattern by lazy { Pattern.compile(Pattern.quote(File.separator)) }
        }
    }

    interface StaticExtractor<R> {
        val value: String
    }
    interface VariableExtractor<K, P> {
        fun pathPartFromKey(key: K): P?
        fun keyValueFromPathPart(part: String): P
    }

    class UuidVariable<K : Key, R : Record<K>>(
            private val extractor: K.() -> UUID
    ) : PathPart<K, R, UUID>(), VariableExtractor<K, UUID> {
        override fun pathPartFromKey(key: K) = extractor(key)
        override fun keyValueFromPathPart(part: String): UUID = UUID.fromString(part)
        override val regex: Pattern = hasUuidPattern
    }

    class StringVariable<K : Key, R : Record<K>>(
            private val extractor: K.() -> String
    ) : PathPart<K, R, String>(), VariableExtractor<K, String> {
        override fun pathPartFromKey(key: K): String = extractor(key)
        override fun keyValueFromPathPart(part: String) = part
        override val regex = alphanumericWithHyphensAndUnderscores

        companion object {
            val alphanumericWithHyphensAndUnderscores: Pattern by lazy {
                Pattern.compile("[\\w-]*")
            }
        }
    }
}