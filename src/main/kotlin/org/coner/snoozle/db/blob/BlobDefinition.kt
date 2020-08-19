package org.coner.snoozle.db.blob

import org.coner.snoozle.db.LiteralRecordDefinition
import org.coner.snoozle.db.Record
import org.coner.snoozle.db.path.PathPart

class BlobDefinition<B : Blob> : LiteralRecordDefinition<Record<B>, B>() {

    var path: List<PathPart<Record<B>, B, *>> = mutableListOf()
    var key: (BlobKeyParser<B>.Context.() -> B)? = null

}