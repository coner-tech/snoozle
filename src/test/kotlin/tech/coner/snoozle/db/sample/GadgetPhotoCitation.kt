package tech.coner.snoozle.db.sample

import tech.coner.snoozle.db.blob.Blob
import java.util.*

data class GadgetPhotoCitation(
        val gadgetId: UUID,
        val id: String
) : Blob