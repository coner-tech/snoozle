package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.RecordDefinition
import org.coner.snoozle.db.PathPart
import java.util.*

class VersionedEntityDefinition<VE : VersionedEntity<EK>, EK : Key>
    : RecordDefinition<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>() {

    var path: List<PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>> = mutableListOf()
    var key: (EntityKeyParser<VE, EK>.Context.() -> EK)? = null

    operator fun String.div(uuidExtractor: EK.() -> UUID): MutableList<PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>> {
        return mutableListOf(
                PathPart.StringValue<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>(this) as PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>,
                PathPart.DirectorySeparator<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>() as PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>,
                PathPart.UuidVariable<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>> { entity.uuidExtractor() } as PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>
        )
    }

    operator fun MutableList<PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>>.div(
            versionArgumentExtractor: VersionArgumentExtractor<VersionedEntityContainerKey<*>>
    ): MutableList<PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>> {
        add(PathPart.DirectorySeparator<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>()  as PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>)
        add(PathPart.VersionArgumentVariable<
                VersionedEntityContainer<VE, EK>,
                VersionedEntityContainerKey<EK>,
                VE,
                EK
                >()  as PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>)
        return this
    }

    operator fun MutableList<PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>>.plus(
            extension: String
    ): MutableList<PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>> {
        add(PathPart.StringValue<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>(extension) as PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>)
        return this
    }

    class VersionArgumentExtractor<VK : VersionedEntityContainerKey<*>>
        : PathArgumentExtractor<VK, Int>({ version })

    val version: VersionArgumentExtractor<VersionedEntityContainerKey<*>>
        get() = VersionArgumentExtractor()
}