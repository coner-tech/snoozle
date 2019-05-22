package org.coner.snoozle.db

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.node.ObjectNode

data class WholeRecord<E : Entity>(
        @JsonProperty("entity") internal val entityObjectNode: ObjectNode? = null,
        @JsonIgnore val entityValue: E,
        val currentVersion: CurrentVersionRecord?,
        val history: List<HistoricVersionRecord<E>>?
) {
    internal class Builder<E : Entity>(
            @JsonProperty("entity") var entityObjectNode: ObjectNode? = null,
            @JsonIgnore var entityValue: E? = null,
            var currentVersion: CurrentVersionRecord? = null,
            var history: List<HistoricVersionRecord<E>>? = null
    ) {
        fun build() = WholeRecord(
                entityObjectNode = entityObjectNode!!,
                entityValue = entityValue!!,
                currentVersion = currentVersion,
                history = history
        )
    }

    internal fun buildUpon() = Builder(
            entityObjectNode = entityObjectNode,
            entityValue = entityValue,
            currentVersion = currentVersion,
            history = history
    )
}
