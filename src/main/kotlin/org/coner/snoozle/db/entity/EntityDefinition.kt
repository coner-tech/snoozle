package org.coner.snoozle.db.entity

import org.coner.snoozle.db.RecordDefinition
import org.coner.snoozle.db.path.PathPart
import org.coner.snoozle.db.versioning.EntityVersioningStrategy
import java.util.*

class EntityDefinition<E : Entity> : RecordDefinition<E>() {
    var versioning: EntityVersioningStrategy? = null
}
