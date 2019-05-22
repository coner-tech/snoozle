package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import java.time.Instant
import java.time.ZonedDateTime

internal class AutomaticEntityVersionIoDelegate<E : Entity>(
        private val reader: ObjectReader,
        private val entityDefinition: EntityDefinition<E>
) : IoDelegate<E> {

    override fun write(old: WholeRecord<E>?, new: WholeRecord.Builder<E>, newContent: E) {
        val newEntityHistoryRecords = mutableListOf<HistoricVersionRecord<E>>()
        old?.history?.run {
            newEntityHistoryRecords.addAll(this)
        }
        old?.run {
            newEntityHistoryRecords.add(
                    HistoricVersionRecord(
                            entityObjectNode = this.entityObjectNode,
                            entityValue = this.entityValue,
                            version = this.currentVersion?.version ?: 0,
                            ts = this.currentVersion?.ts ?: ZonedDateTime.from(Instant.EPOCH)
                    )
            )
        }
        val priorVersion = newEntityHistoryRecords.lastOrNull()
        val newCurrentVersionRecord = CurrentVersionRecord(
                version = (priorVersion?.version ?: -1) + 1,
                ts = ZonedDateTime.now()
        )
        new.apply {
            history = newEntityHistoryRecords.toList()
            currentVersion = newCurrentVersionRecord
        }
    }

    override fun read(wholeRecord: WholeRecord.Builder<E>) {
        wholeRecord.history = wholeRecord.history?.map { historicRecord ->
            if (historicRecord.entityObjectNode == null) throw EntityIoException.ReadFailure(
                    "Historic record missing entity, unable to read"
            )
            historicRecord.copy(entityValue = reader.treeToValue(
                    historicRecord.entityObjectNode,
                    entityDefinition.kClass.java
            ))
        }
    }

}