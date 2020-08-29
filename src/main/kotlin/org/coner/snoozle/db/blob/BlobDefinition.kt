package org.coner.snoozle.db.blob

import org.coner.snoozle.db.LiteralRecordDefinition
import org.coner.snoozle.db.Record
import org.coner.snoozle.db.PathPart
import kotlin.reflect.KClass

class BlobDefinition<B : Blob>(
        blobClass: KClass<B>
) : LiteralRecordDefinition<B, Record<B>>(blobClass, blobClass as KClass<Record<B>>) {

    var key: (BlobKeyParser<B>.Context.() -> B)? = null

}