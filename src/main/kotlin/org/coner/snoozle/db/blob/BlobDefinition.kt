package org.coner.snoozle.db.blob

import org.coner.snoozle.db.LiteralRecordDefinition
import org.coner.snoozle.db.Record

class BlobDefinition<B : Blob> : LiteralRecordDefinition<Record<B>, B>() {

    lateinit var factory: BlobFactory<B>

}