package org.coner.snoozle.db

data class WholeRecord<E : Entity>(
        val entity: E,
        val currentVersion: CurrentVersionRecord?,
        val history: List<HistoricVersionRecord<E>>?
)