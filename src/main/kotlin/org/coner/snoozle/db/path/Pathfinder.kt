package org.coner.snoozle.db.path

import java.nio.file.Path
import java.nio.file.Paths

class Pathfinder<R>(
        private val pathParts: List<PathPart<R>>
) {

    private val recordVariablePathParts by lazy {
        pathParts.filter { it is PathPart.VariablePathPart<*> }
    }
    fun findRecordByArgs(vararg args: Any): Path {
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
        val mappedRelativePath = pathParts.joinToString(separator = "") { pathPart ->
            pathPart.forRecord(record)
        }
        return Paths.get(mappedRelativePath)
    }

    private val listingPathParts by lazy {
        pathParts.take(pathParts.indexOfLast { it is PathPart.DirectorySeparatorPathPart<R> })
    }
    private val listingVariablePathPartsCount by lazy {
        listingPathParts.count { it is PathPart.VariablePathPart<*> }
    }

    fun findListingByArgs(vararg args: Any?): Path {
        check(args.size == listingVariablePathPartsCount) {
            "args.size (${args.count()}) doesn't match listingVariablePathPartsCount ($listingVariablePathPartsCount)"
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

    fun findListingByRecord(record: R): Path {
        val mappedRelativePath = listingPathParts.joinToString(separator = "") { pathPart ->
            pathPart.forRecord(record)
        }
        return Paths.get(mappedRelativePath)
    }

    private val directorySeparatorPathPart by lazy { PathPart.DirectorySeparatorPathPart<R>() }

    fun isRecord(candidate: Path): Boolean {
        return try {
            var remainingCandidateParts = candidate.toString()
            for (pathPart in pathParts) {
                val matcher = pathPart.regex.matcher(remainingCandidateParts)
                if (!matcher.find()) {
                    break
                }
                if (matcher.start() != 0) {
                    break
                }
                remainingCandidateParts = remainingCandidateParts.substring(matcher.end())
            }
            return remainingCandidateParts.isEmpty()
        } catch (t: Throwable) {
            false
        }
    }

}