package org.coner.snoozle.db.blob

import org.coner.snoozle.db.LiteralRecordDefinition

class BlobDefinition<B : Blob> : LiteralRecordDefinition<B>() {

    lateinit var factory: BlobFactory<B>

}