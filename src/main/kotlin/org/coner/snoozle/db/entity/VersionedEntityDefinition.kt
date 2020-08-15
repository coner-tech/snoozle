package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.RecordDefinition
import org.coner.snoozle.db.path.PathPart
import java.util.*

class VersionedEntityDefinition<VE : VersionedEntity<EK>, EK : Key>
    : RecordDefinition<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>() {

    operator fun String.div(uuidExtractor: EK.() -> UUID): MutableList<PathPart<VersionedEntityContainer<VE, EK>>> {
        return mutableListOf(
                PathPart.StringValue(this),
                PathPart.DirectorySeparator(),
                PathPart.UuidVariable { entity.key.uuidExtractor() }
        )
    }

    operator fun MutableList<PathPart<VersionedEntityContainer<VE, EK>>>.div(
            versionArgumentExtractor: VersionArgumentExtractor<VersionedEntityContainerKey<*>>
    ): MutableList<PathPart<VersionedEntityContainer<VE, EK>>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.VersionArgumentVariable())
        return this
    }

    operator fun MutableList<PathPart<VersionedEntityContainer<VE, EK>>>.plus(
            extension: String
    ): MutableList<PathPart<VersionedEntityContainer<VE, EK>>> {
        add(PathPart.StringValue(extension))
        return this
    }

    class VersionArgumentExtractor<VK : VersionedEntityContainerKey<*>>
        : PathArgumentExtractor<VK, Int>({ version })

    val version: VersionArgumentExtractor<VersionedEntityContainerKey<*>>
        get() = VersionArgumentExtractor()
}