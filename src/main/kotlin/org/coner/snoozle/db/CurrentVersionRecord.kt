package org.coner.snoozle.db

import java.time.ZonedDateTime

data class CurrentVersionRecord(
        val version: Int,
        val ts: ZonedDateTime
)