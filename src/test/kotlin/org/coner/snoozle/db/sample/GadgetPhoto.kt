package org.coner.snoozle.db.sample

import org.coner.snoozle.db.blob.Blob
import java.util.*

class GadgetPhoto(
        val gadgetId: UUID,
        val id: String,
        val extension: String
) : Blob