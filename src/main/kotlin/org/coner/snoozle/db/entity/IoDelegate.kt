package org.coner.snoozle.db.entity

internal interface IoDelegate<E : Entity> {

    fun write(old: WholeRecord<E>?, new: WholeRecord.Builder<E>, newContent: E)

    fun read(wholeRecord: WholeRecord.Builder<E>)
}