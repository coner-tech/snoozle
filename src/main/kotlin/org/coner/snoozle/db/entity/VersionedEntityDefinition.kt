package org.coner.snoozle.db.entity

import org.coner.snoozle.db.RecordDefinition
import org.coner.snoozle.db.path.PathPart
import java.util.*
import kotlin.reflect.KClass

class VersionedEntityDefinition<VE : VersionedEntity, VC : VersionedEntityContainer<VE>> : RecordDefinition<VC>() {

    operator fun String.div(uuidExtractor: VE.() -> UUID): MutableList<PathPart<VC>> {
        return mutableListOf(
                PathPart.StringValue(this),
                PathPart.DirectorySeparator(),
                PathPart.UuidVariable { entity.uuidExtractor() }
        )
    }

    operator fun MutableList<PathPart<VC>>.div(
            versionArgumentExtractor: VersionArgumentExtractor<VC>
    ): MutableList<PathPart<VC>> {
        add(PathPart.DirectorySeparator())
        add(PathPart.VersionArgumentVariable())
        return this
    }

    operator fun MutableList<PathPart<VC>>.plus(
            extension: String
    ): MutableList<PathPart<VC>> {
        add(PathPart.StringValue(extension))
        return this
    }

    class VersionArgumentExtractor<VC : VersionedEntityContainer<*>>
        : PathArgumentExtractor<VC, Int>({ version })

    val version: VersionArgumentExtractor<VC>
        get() = VersionArgumentExtractor()
}