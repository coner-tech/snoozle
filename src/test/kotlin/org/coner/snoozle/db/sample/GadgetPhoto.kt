package org.coner.snoozle.db.sample

import org.coner.snoozle.db.blob.Blob
import org.coner.snoozle.db.blob.LambdaBlobFactory
import org.coner.snoozle.util.extension
import org.coner.snoozle.util.nameWithoutExtension
import org.coner.snoozle.util.uuid
import java.util.*

class GadgetPhoto(
        val gadgetId: UUID,
        val id: String,
        val extension: String
) : Blob {

    class Factory : LambdaBlobFactory<GadgetPhoto>({
        GadgetPhoto(
                gadgetId = uuid(it.getName(1).toString()),
                id = it.getName(3).nameWithoutExtension,
                extension = it.fileName.extension
        )
    })
}