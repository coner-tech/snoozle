package org.coner.snoozle.db

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.ZonedDateTime

data class HistoricVersionRecord<E : Entity>(
        val version: Int,
        val ts: ZonedDateTime,
        @JsonProperty("entity") val _entityObjectNode: ObjectNode? = null,
        @JsonIgnore val entityValue: E?
)