package org.coner.snoozle.db.path

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import java.util.regex.Pattern
import java.util.stream.Stream

open class Pathfinder<R>(
        protected val root: Path,
        protected val pathParts: List<PathPart<R>>
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

    private val recordCandidatePath: Pattern by lazy {
        val joined = pathParts.map { it.regex.pattern() }.joinToString("")
        Pattern.compile("^$joined$")
    }

    fun isRecord(candidate: Path): Boolean {
        return recordCandidatePath.matcher(candidate.toString()).matches()
    }

    protected val listingStart: Path by lazy {
        val firstIndexOfVariablePathPart = pathParts.indexOfFirst { it is PathPart.VariableExtractor<*> }
        val staticPathParts = pathParts.take(firstIndexOfVariablePathPart)
        staticPathParts.fold(root) { accumulator, pathPart ->
            when (pathPart) {
                is PathPart.DirectorySeparator -> accumulator
                else -> accumulator.resolve(pathPart.extractQueryArgument(null))
            }
        }
    }

    protected val listingMaxDepth: Int by lazy {
        val firstIndexOfVariablePathPart = pathParts.indexOfFirst { it is PathPart.VariableExtractor<*> }
        pathParts.subList(firstIndexOfVariablePathPart, pathParts.lastIndex)
                .count { it is PathPart.DirectorySeparator }
    }

    open fun listAll(): Stream<Path> {
        return Files.find(listingStart, listingMaxDepth, BiPredicate { candidate: Path, attrs: BasicFileAttributes ->
                    attrs.isDirectory && isRecord(root.relativize(candidate))
        } ).sorted()
    }

}