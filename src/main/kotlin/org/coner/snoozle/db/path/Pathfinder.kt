package org.coner.snoozle.db.path

import java.nio.file.Path
import java.nio.file.Paths

class Pathfinder<R>(
        private val pathParts: List<PathPart<R>>
) {

    private val recordVariablePathParts by lazy {
        pathParts.filter { it is PathPart.VariablePathPart<*> }
    }
    fun findRecord(vararg args: Any): Path {
        check(args.size == recordVariablePathParts.size) {
            "args.size (${args.size}) doesn't match recordVariablePathParts.size (${recordVariablePathParts.size})"
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

    private val listingPathParts by lazy {
        pathParts.take(pathParts.indexOfLast { it is PathPart.DirectorySeparatorPathPart<R> })
    }
    private val listingVariablePathPartsCount by lazy {
        listingPathParts.count { it is PathPart.VariablePathPart<*> }
    }

    fun findListing(vararg args: Any): Path {
        check(args.size == listingVariablePathPartsCount) {
            "args.size (${args.size}) doesn't match listingVariablePathPartsCount ($listingVariablePathPartsCount)"
        }
        val argsIterator = args.iterator()
        val mappedRelativePath = listingPathParts.map { pathPart ->
            pathPart to when (pathPart) {
                is PathPart.VariablePathPart<*> -> argsIterator.next()
                else -> null
            }
        }.joinToString(separator = "") { (pathPart, arg) -> pathPart.extractQueryArgument(arg) }
        return Paths.get(mappedRelativePath)
    }

}