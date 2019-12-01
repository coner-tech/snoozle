package org.coner.snoozle.db

import org.coner.snoozle.db.path.PathPart

class EntityDefinition<E : Entity> {
    private var pathParts: List<PathPart> = mutableListOf()
}
