package org.coner.snoozle.db.path

import java.io.File
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
        val mappedRelativePath: String = variablePathParts
                .zip(args)
                .mapIndexed { index, (variablePathPart, arg) -> when (arg) {
                    is String -> arg
                    is UUID -> arg.toString()
                    else -> throw UnsupportedOperationException(
                            "No handling implemented for arg[$index] of type ${arg::class.qualifiedName}"
                    )
                } }
                .joinToString(separator = File.separator)
        return Paths.get(mappedRelativePath)
    }

    fun findRecord(record: R): Path {
        TODO()
    }

    fun findListing(): Path {
        TODO()
    }
}