package org.coner.snoozle.db.blob

import org.coner.snoozle.db.RecordDefinition

class BlobDefinition<B : Blob> : RecordDefinition<B>() {

    lateinit var factory: BlobFactory<B>

}