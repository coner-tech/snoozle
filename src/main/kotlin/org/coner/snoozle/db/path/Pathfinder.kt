package org.coner.snoozle.db.path

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

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

    fun findVersions(vararg args: Any): Path {
        val argsIterator = args.iterator()
        val pathPartsIterator = pathParts.listIterator()
        var foundVersionArgument = false
        val mappedRelativePath = buildString {
            while (!foundVersionArgument && argsIterator.hasNext()) {
                var foundVariablePathPart = false
                 while (!foundVariablePathPart && !foundVersionArgument) {
                    val pathPart = pathPartsIterator.next()
                    when (pathPart) {
                        is PathPart.VersionArgumentVariable -> {
                            foundVersionArgument = true
                        }
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

    private val recordCandidatePath: Pattern by lazy {
        val joined = pathParts.map { it.regex.pattern() }
                .joinToString("")
        Pattern.compile("^$joined$")
    }

    fun isRecord(candidate: Path): Boolean {
        return recordCandidatePath.matcher(candidate.toString()).matches()
    }

    private val versionedEntityContainerListingPathParts by lazy {
        pathParts.takeWhile { it !is PathPart.VersionArgumentVariable }
    }

    private val versionedEntityContainerListingCandidatePath: Pattern by lazy {
        val indexOfLastDirectorySeparator = pathParts.indexOfLast { it is PathPart.DirectorySeparator }
        val joined = pathParts.take(indexOfLastDirectorySeparator).joinToString("") { it.regex.pattern() }
        Pattern.compile("^$joined$")
    }

    fun isVersionedEntityContainerListing(candidate: Path): Boolean {
        return versionedEntityContainerListingCandidatePath.matcher(candidate.toString()).matches()
    }

    fun extractArgsWithoutVersion(versionListing: Path): Array<Any> {
        var remainingPathParts = versionListing.toString()
        val args = mutableListOf<Any>()
        versionedEntityContainerListingPathParts.forEachIndexed { index, pathPart ->
            if (index == versionedEntityContainerListingPathParts.lastIndex && pathPart is PathPart.DirectorySeparator) return@forEachIndexed
            val matcher = pathPart.regex.matcher(remainingPathParts)
            require(matcher.find()) { "Only use with paths that have already validated with isVersionedEntityContainerListing(Path)" }
            args += pathPart.produceQueryArgument(matcher.group()) ?: return@forEachIndexed
            remainingPathParts = remainingPathParts.substring(matcher.end())
        }
        return args.toTypedArray()
    }

}