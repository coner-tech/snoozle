package org.coner.snoozle.db.blob

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.Record

interface Blob : Record<Blob>, Key {

    override val key: Blob
        get() = this
}