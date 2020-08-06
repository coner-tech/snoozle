package org.coner.snoozle.db.entity

import java.time.ZonedDateTime

@Deprecated("Use discrete version")
data class CurrentVersionRecord(
        val version: Int,
        val ts: ZonedDateTime
)