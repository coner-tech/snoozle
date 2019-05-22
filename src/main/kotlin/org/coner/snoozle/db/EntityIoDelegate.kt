package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.node.ObjectNode

internal class EntityIoDelegate<E : Entity>(
        private val objectMapper: ObjectMapper,
        private val reader: ObjectReader,
        private val writer: ObjectWriter
) : IoDelegate<E> {

    override fun write(old: WholeRecord<E>?, new: WholeRecord.Builder<E>, newContent: E) {
        new.apply {
            entityValue = newContent
            entityObjectNode = objectMapper.valueToTree(newContent)
        }
    }

    override fun read(wholeRecord: WholeRecord.Builder<E>) {
        wholeRecord.apply {
            entityValue = reader.readValue(wholeRecord.entityObjectNode)
        }
    }
}