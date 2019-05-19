package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.node.ObjectNode

class EntityIoDelegate<E : Entity>(
        private val objectMapper: ObjectMapper,
        private val reader: ObjectReader,
        private val writer: ObjectWriter
) : IoDelegate<E> {

    private val entityField = "entity"

    override val fields = listOf(entityField)

    override fun write(oldRoot: ObjectNode?, newRoot: ObjectNode, newContent: E) {
        newRoot.set(entityField, objectMapper.valueToTree(newContent))
    }

    override fun read(root: ObjectNode): E {
        return reader.readValue(root.get(entityField))
    }
}