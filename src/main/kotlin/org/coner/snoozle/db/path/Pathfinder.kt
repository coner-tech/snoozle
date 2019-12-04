package org.coner.snoozle.db.path

import java.nio.file.Path

class Pathfinder<R>(
        private val pathParts: List<PathPart<R>>
) {

    fun findRecord(vararg args: Any): Path {
        TODO()
    }

    fun findRecord(record: R): Path {
        TODO()
    }

    fun findListing(): Path {
        TODO()
    }
}