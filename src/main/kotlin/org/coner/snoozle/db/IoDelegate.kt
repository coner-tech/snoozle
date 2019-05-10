package org.coner.snoozle.db

import com.fasterxml.jackson.databind.node.ObjectNode

interface IoDelegate<T> {

    val field: String

    fun write(oldRoot: ObjectNode?, newRoot: ObjectNode, newContent: T)

    fun read(root: ObjectNode): T
}