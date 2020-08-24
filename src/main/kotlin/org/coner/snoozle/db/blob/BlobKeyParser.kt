package org.coner.snoozle.db.blob

import org.coner.snoozle.db.Record
import org.coner.snoozle.db.PathPart
import java.util.*

class BlobKeyParser<B : Blob>(
        private val uuidPathPartExtractors: Array<PathPart.UuidVariable<Record<B>, B>?>,
        private val stringPathPartExtractors: Array<PathPart.StringVariable<Record<B>, B>?>,
        private val fn: BlobKeyParser<B>.Context.() -> B
) {

    fun parse(rawStringParts: Array<String>): B {
        return fn.invoke(Context(rawStringParts))
    }

    inner class Context(
            private val rawStringParts: Array<String>
    ) {
        fun uuidAt(index: Int): UUID {
            return uuidPathPartExtractors[index]?.keyValueFromPathPart(rawStringParts[index])
                    ?: throw IllegalArgumentException("No UuidVariable at index $index")
        }

        fun stringAt(index: Int): String {
            return stringPathPartExtractors[index]?.keyValueFromPathPart(rawStringParts[index])
                    ?: throw IllegalArgumentException("No StringVariable at index $index")
        }
    }

}
