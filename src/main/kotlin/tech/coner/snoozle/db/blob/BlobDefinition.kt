package tech.coner.snoozle.db.blob

import tech.coner.snoozle.db.LiteralRecordDefinition
import tech.coner.snoozle.db.Record
import kotlin.reflect.KClass

class BlobDefinition<B : Blob>(
        blobClass: KClass<B>
) : LiteralRecordDefinition<B, Record<B>>(blobClass, blobClass as KClass<Record<B>>) {

    var key: (BlobKeyParser<B>.Context.() -> B)? = null

}