package org.coner.snoozle.db.entity

import org.coner.snoozle.db.RecordDefinition
import org.coner.snoozle.db.versioning.EntityVersioningStrategy

class EntityDefinition<E : Entity> : RecordDefinition<E>() {
    @Deprecated("Use VersionedEntity instead")
    var versioning: EntityVersioningStrategy? = null
}
