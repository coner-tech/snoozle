package org.coner.snoozle.db.sample

import org.coner.snoozle.db.blob.Blob
import org.coner.snoozle.db.blob.LambdaBlobFactory
import org.coner.snoozle.util.nameWithoutExtension
import org.coner.snoozle.util.uuid
import java.util.*

data class GadgetPhotoCitation(
        val gadgetId: UUID,
        val id: String
) : Blob {

    class Factory : LambdaBlobFactory<GadgetPhotoCitation>({
        GadgetPhotoCitation(
                gadgetId = uuid(it.getName(1).toString()),
                id = it.getName(4).nameWithoutExtension
        )
    })
}