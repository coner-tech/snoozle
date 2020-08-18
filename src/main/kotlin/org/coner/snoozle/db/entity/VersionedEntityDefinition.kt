package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.RecordDefinition
import org.coner.snoozle.db.path.PathPart
import java.util.*

class VersionedEntityDefinition<VE : VersionedEntity<EK>, EK : Key>
    : RecordDefinition<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>() {

    operator fun String.div(uuidExtractor: EK.() -> UUID): MutableList<PathPart<VersionedEntityContainer<VE, EK>, EK, *>> {
        return mutableListOf(
                PathPart.StringValue<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>(this) as PathPart<VersionedEntityContainer<VE, EK>, EK, *>,
                PathPart.DirectorySeparator<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>() as PathPart<VersionedEntityContainer<VE, EK>, EK, *>,
                PathPart.UuidVariable<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>> { entity.key.uuidExtractor() } as PathPart<VersionedEntityContainer<VE, EK>, EK, *>
        )
    }

    operator fun MutableList<PathPart<VersionedEntityContainer<VE, EK>, EK, *>>.div(
            versionArgumentExtractor: VersionArgumentExtractor<VersionedEntityContainerKey<*>>
    ): MutableList<PathPart<VersionedEntityContainer<VE, EK>, EK, *>> {
        add(PathPart.DirectorySeparator<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>()  as PathPart<VersionedEntityContainer<VE, EK>, EK, *>)
        add(PathPart.VersionArgumentVariable<
                VersionedEntityContainer<VE, EK>,
                VersionedEntityContainerKey<EK>,
                VE,
                EK
                >()  as PathPart<VersionedEntityContainer<VE, EK>, EK, *>)
        return this
    }

    operator fun MutableList<PathPart<VersionedEntityContainer<VE, EK>, EK, *>>.plus(
            extension: String
    ): MutableList<PathPart<VersionedEntityContainer<VE, EK>, EK, *>> {
        add(PathPart.StringValue<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>(extension) as PathPart<VersionedEntityContainer<VE, EK>, EK, *>)
        return this
    }

    class VersionArgumentExtractor<VK : VersionedEntityContainerKey<*>>
        : PathArgumentExtractor<VK, Int>({ version })

    val version: VersionArgumentExtractor<VersionedEntityContainerKey<*>>
        get() = VersionArgumentExtractor()
}