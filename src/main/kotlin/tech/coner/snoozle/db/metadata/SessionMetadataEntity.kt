package tech.coner.snoozle.db.metadata

import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.session.Session
import tech.coner.snoozle.db.session.administrative.AdministrativeSession
import tech.coner.snoozle.db.session.data.DataSession
import java.net.InetAddress
import java.util.*

data class SessionMetadataEntity(
    val id: UUID,
    val host: String,
    val processId: Long,
    val type: Type
) : Entity<SessionMetadataEntity.Key> {

    data class Key(val id: UUID) : tech.coner.snoozle.db.Key

    enum class Type {
        ADMINISTRATIVE,
        DATA
    }
}

typealias SessionMetadataResource = EntityResource<SessionMetadataEntity.Key, SessionMetadataEntity>

fun Session.toSessionMetadataEntity() = SessionMetadataEntity(
    id = id,
    host = InetAddress.getLocalHost().canonicalHostName,
    processId = ProcessHandle.current().pid(),
    type = when (this) {
        is AdministrativeSession -> SessionMetadataEntity.Type.ADMINISTRATIVE
        is DataSession -> SessionMetadataEntity.Type.DATA
        else -> throw Exception("Unknown session type")
    }
)

fun Session.toSessionMetadataKey() = SessionMetadataEntity.Key(id = id)