package org.coner.snoozle.db

import com.fasterxml.jackson.databind.node.ObjectNode

interface IoDelegate<E : Entity, T> {

4    fun write(old: WholeRecord<E>?, new: WholeRecord<E>, newContent: E): WholeRecord<E>
}