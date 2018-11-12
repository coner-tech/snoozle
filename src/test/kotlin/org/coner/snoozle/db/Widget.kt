package org.coner.snoozle.db

import java.util.*

@EntityPath("/widgets/{id}")
data class Widget(
        val id: UUID,
        val name: String
) : Entity