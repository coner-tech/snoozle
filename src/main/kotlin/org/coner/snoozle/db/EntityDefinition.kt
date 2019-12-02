package org.coner.snoozle.db

import org.coner.snoozle.db.path.PathPart
import org.coner.snoozle.db.path.StringPathPart
import org.coner.snoozle.db.path.UuidPathPart
import java.util.*

class EntityDefinition<E : Entity> {
    var path: List<PathPart<E>> = mutableListOf()
    var versioning: EntityVersioningStrategy? = null

    operator fun String.div(extractor: (E) -> UUID): MutableList<PathPart<E>> {
        return mutableListOf(
                StringPathPart(this),
                UuidPathPart(extractor)
        )
    }

    operator fun MutableList<PathPart<E>>.div(part: String): MutableList<PathPart<E>> {
        add(StringPathPart(part))
        return this
    }

    operator fun MutableList<PathPart<E>>.div(extractor: (E) -> UUID): MutableList<PathPart<E>> {
        add(UuidPathPart(extractor))
        return this
    }

}
