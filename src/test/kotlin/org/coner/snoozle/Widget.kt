package org.coner.snoozle

import org.coner.snoozle.annotations.Entity
import org.coner.snoozle.annotations.Id

@Entity(
        path = "/widgets/{id}"
)
data class Widget(
        @Id val id: String,
        val name: String
)