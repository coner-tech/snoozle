package org.coner.snoozle.db.entity

import java.time.ZonedDateTime

data class CurrentVersionRecord(
        val version: Int,
        val ts: ZonedDateTime
)