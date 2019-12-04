package org.coner.snoozle.db.path

import java.util.*

sealed class PathPart<E> {
    class StringPathPart<E>(val part: String) : PathPart<E>()
    class UuidPathPart<E>(val extractor: (E) -> UUID) : VariablePathPart<E>
    interface VariablePathPart<E>
}