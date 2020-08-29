package org.coner.snoozle.db.sample

import org.coner.snoozle.db.blob.Blob
import java.util.*

data class GadgetPhotoCitation(
        val gadgetId: UUID,
        val id: String
) : Blob