package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.blob.Blob
import java.util.*

class GadgetPhoto(
        val gadgetId: UUID,
        val id: String,
        val extension: String
) : Blob