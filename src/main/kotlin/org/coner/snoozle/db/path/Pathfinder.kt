package org.coner.snoozle.db.path

import java.nio.file.Path
import java.nio.file.Paths

class Pathfinder<R>(
        private val pathParts: List<PathPart<R>>
) {

    private val recordVariablePathParts by lazy {
        pathParts.filter { it is PathPart.VariableExtractor<*> }
    }
    fun findRecordByArgs(vararg args: Any): Path {
        check(args.size == recordVariablePathParts.size) {
            "args.size (${args.size}) doesn't match recordVariablePathParts.size (${recordVariablePathParts.size})"
        }
        val argsIterator = args.iterator()
        val mappedRelativePath = pathParts.map { pathPart ->
            pathPart to when (pathPart) {
                is PathPart.VariableExtractor<*> -> argsIterator.next()
                else -> null
            }
        }.joinToString(separator = "") { (pathPart, arg) -> pathPart.extractQueryArgument(arg) }
        return Paths.get(mappedRelativePath)
    }

    fun findPartialBySubsetArgs(vararg args: Any): Path {
        check(args.size < recordVariablePathParts.size) {
            "args.size (${args.size}) must be less than recordVariablePathParts.size (${recordVariablePathParts.size})"
        }
        val argsIterator = args.iterator()
        val pathPartsIterator = pathParts.listIterator()
        val mappedRelativePath = buildString {
            while (argsIterator.hasNext()) {
                var foundVariablePathPart = false
                while (!foundVariablePathPart) {
                    val pathPart = pathPartsIterator.next()
                    when (pathPart) {
                        is PathPart.VariableExtractor<*> -> {
                            append(pathPart.extractQueryArgument(argsIterator.next()))
                            foundVariablePathPart = true
                        }
                        else -> append(pathPart.extractQueryArgument(null))
                    }
                }
            }
        }
        return Paths.get(mappedRelativePath)
    }

    fun findRecord(record: R): Path {
        val mappedRelativePath = pathParts.joinToString(separator = "") { pathPart ->
            pathPart.forRecord(record)
        }
        return Paths.get(mappedRelativePath)
    }

    private val listingPathParts by lazy {
        pathParts.take(pathParts.indexOfLast { it is PathPart.DirectorySeparator<R> })
    }
    private val listingVariablePathPartsCount by lazy {
        listingPathParts.count { it is PathPart.VariableExtractor<*> }
    }

    fun findListingByArgs(vararg args: Any?): Path {
        check(args.size == listingVariablePathPartsCount) {
            "args.size (${args.count()}) doesn't match listingVariablePathPartsCount ($listingVariablePathPartsCount)"
        }
        val argsIterator = args.iterator()
        val mappedRelativePath = listingPathParts.map { pathPart ->
            pathPart to when (pathPart) {
                is PathPart.VariableExtractor<*> -> argsIterator.next()
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

    private val directorySeparatorPathPart by lazy { PathPart.DirectorySeparator<R>() }

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