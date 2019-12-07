package org.coner.snoozle.db

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode

data class WholeRecord<E : Entity>(
        @JsonProperty("entity") val _entityObjectNode: ObjectNode? = null,
        @JsonIgnore val entityValue: E,
        val currentVersion: CurrentVersionRecord?,
        val history: List<HistoricVersionRecord<E>>?
) {
    class Builder<E : Entity>(
            @JsonProperty("entity") var _entityObjectNode: ObjectNode? = null,
            @JsonIgnore var entityValue: E? = null,
            var currentVersion: CurrentVersionRecord? = null,
            var history: List<HistoricVersionRecord<E>>? = null
    ) {
        fun build() = WholeRecord(
                _entityObjectNode = _entityObjectNode!!,
                entityValue = entityValue!!,
                currentVersion = currentVersion,
                history = history
        )
    }

    fun buildUpon() = Builder(
            _entityObjectNode = _entityObjectNode,
            entityValue = entityValue,
            currentVersion = currentVersion,
            history = history
    )
}
