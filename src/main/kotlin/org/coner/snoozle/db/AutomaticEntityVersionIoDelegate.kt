package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import java.time.Instant
import java.time.ZonedDateTime

class AutomaticEntityVersionIoDelegate<E : Entity>(
        private val objectMapper: ObjectMapper,
        private val entityIoDelegate: EntityIoDelegate<E>
) : IoDelegate<E> {

    private val historyField = "history"
    private val currentVersionField = "currentVersion"

    override val fields = listOf(historyField, currentVersionField)

    override fun write(oldRoot: ObjectNode?, newRoot: ObjectNode, newContent: E) {
        val oldCurrentVersionNode = oldRoot?.get(currentVersionField)
        val oldCurrentVersionRecord: CurrentVersionRecord? = oldCurrentVersionNode?.run {
            try {
                objectMapper.treeToValue<CurrentVersionRecord>(this)
            } catch (t: Throwable) {
                throw EntityIoException("Failed to get old current version record", t)
            }
        }
        val oldHistoryNode = oldRoot?.get(historyField)
        val oldEntityHistoryRecords: List<HistoricVersionRecord<E>>? = oldHistoryNode?.run {
            try {
                objectMapper.treeToValue<List<HistoricVersionRecord<E>>>(this)
            } catch (t: Throwable) {
                throw EntityIoException("Failed to get old historic version records", t)
            }
        }

        val newEntityHistoryRecords = mutableListOf<HistoricVersionRecord<E>>()
        oldEntityHistoryRecords?.run {
            newEntityHistoryRecords.addAll(this)
        }
        oldRoot?.let { oldRootNode ->
            newEntityHistoryRecords.add(
                    HistoricVersionRecord(
                            entity = entityIoDelegate.read(oldRootNode),
                            version = oldCurrentVersionRecord?.version ?: 0,
                            ts = oldCurrentVersionRecord?.ts ?: ZonedDateTime.from(Instant.EPOCH)
                    )
            )
        }
        val priorVersion = newEntityHistoryRecords.lastOrNull()
        val newCurrentVersionRecord = CurrentVersionRecord(
                version = (priorVersion?.version ?: -1) + 1,
                ts = ZonedDateTime.now()
        )
        newRoot.set(historyField, objectMapper.valueToTree(newEntityHistoryRecords))
        newRoot.set(currentVersionField, objectMapper.valueToTree(newCurrentVersionRecord))
    }

    override fun read(root: ObjectNode): E {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}