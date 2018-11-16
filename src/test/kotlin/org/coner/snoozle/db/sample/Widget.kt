package org.coner.snoozle.db.sample

import org.coner.snoozle.db.Entity
import org.coner.snoozle.db.EntityPath
import java.util.*

@EntityPath("/widgets/{id}")
data class Widget(
        val id: UUID,
        val name: String
) : Entity