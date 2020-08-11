package org.coner.snoozle.db.entity

import org.coner.snoozle.db.RecordDefinition
import org.coner.snoozle.db.path.PathPart
import java.util.*
import kotlin.reflect.KClass

class VersionedEntityDefinition<VE : VersionedEntity> : RecordDefinition<VersionedEntityContainer<VE>>() {

    operator fun String.div(uuidExtractor: VE.() -> UUID): MutableList<PathPart<VersionedEntityContainer<VE>>> {
        return mutableListOf(
                PathPart.StringValue(this),
                PathPart.DirectorySeparator(),
                PathPart.UuidVariable { entity.uuidExtractor() }
        )
    }

    operator fun MutableList<PathPart<VersionedEntityContainer<VE>>>.div(
            versionArgumentExtractor: VersionArgumentExtractor<VersionedEntityContainer<VE>>
    ): MutableList<PathPart<VersionedEntityContainer<VE>>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.VersionArgumentVariable())
        return this
    }

    operator fun MutableList<PathPart<VersionedEntityContainer<VE>>>.plus(
            extension: String
    ): MutableList<PathPart<VersionedEntityContainer<VE>>> {
        add(PathPart.StringValue(extension))
        return this
    }

    class VersionArgumentExtractor<VC : VersionedEntityContainer<*>>
        : PathArgumentExtractor<VC, Int>({ version })

    val version: VersionArgumentExtractor<VersionedEntityContainer<VE>>
        get() = VersionArgumentExtractor()
}