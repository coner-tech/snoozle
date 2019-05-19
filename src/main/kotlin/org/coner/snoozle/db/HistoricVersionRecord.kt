package org.coner.snoozle.db

import java.time.ZonedDateTime

data class HistoricVersionRecord<E : Entity>(
        val version: Int,
        val ts: ZonedDateTime,
        val entity: E
)