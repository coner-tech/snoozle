package org.coner.snoozle.db.path

import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class Pathfinder<R>(
        private val pathParts: List<PathPart<R>>
) {

    private val variablePathParts by lazy { pathParts.filter { it is PathPart.VariablePathPart<*> } }

    fun findRecord(vararg args: Any): Path {
        check(args.size == variablePathParts.size) {
            "args.size (${args.size}) doesn't match variablePathParts.size (${args.size})"
        }
        val argsIterator = args.iterator()
        val mappedRelativePath = pathParts.map { pathPart ->
            pathPart to when (pathPart) {
                is PathPart.VariablePathPart<*> -> argsIterator.next()
                else -> null
            }
        }.joinToString(separator = "") { (pathPart, arg) -> pathPart.extractQueryArgument(arg) }
        return Paths.get(mappedRelativePath)
    }

    fun findRecord(record: R): Path {
        TODO()
    }

    fun findListing(vararg args: Any): Path {
        TODO()
    }

}