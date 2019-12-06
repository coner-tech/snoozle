package org.coner.snoozle.db

import org.coner.snoozle.db.path.PathPart
import org.coner.snoozle.db.versioning.EntityVersioningStrategy
import java.util.*

class EntityDefinition<E : Entity> {
    var path: List<PathPart<E>> = mutableListOf()
    var versioning: EntityVersioningStrategy? = null

    operator fun String.div(extractor: (E) -> UUID): MutableList<PathPart<E>> {
        return mutableListOf(
                PathPart.StringPathPart(this),
                PathPart.DirectorySeparatorPathPart(),
                PathPart.UuidPathPart(extractor)
        )
    }

    operator fun MutableList<PathPart<E>>.div(part: String): MutableList<PathPart<E>> {
        add(PathPart.StringPathPart(part))
        add(PathPart.DirectorySeparatorPathPart())
        return this
    }

    operator fun MutableList<PathPart<E>>.div(extractor: (E) -> UUID): MutableList<PathPart<E>> {
        add(PathPart.UuidPathPart(extractor))
        add(PathPart.DirectorySeparatorPathPart())
        return this
    }

    operator fun MutableList<PathPart<E>>.plus(extension: String): MutableList<PathPart<E>> {
        add(PathPart.StringPathPart(extension))
        return this
    }

}
