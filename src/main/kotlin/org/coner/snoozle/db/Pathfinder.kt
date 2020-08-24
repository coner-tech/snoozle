package org.coner.snoozle.db

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import java.util.regex.Pattern
import java.util.stream.Stream

open class Pathfinder<R : Record<K>, K : Key>(
        protected val root: Path,
        protected val pathParts: List<PathPart<R, K, *>>
) {

    private val recordVariablePathParts by lazy {
        pathParts.filter { it is PathPart.VariableExtractor<*> }
    }

    fun findRecord(key: K): Path {
        val relativePath = pathParts.joinToString(separator = "") { pathPart ->
            pathPart.pathPartFromKey(key).toString()
        }
        return Paths.get(relativePath)
    }

    fun findRecord(record: R): Path {
        return findRecord(record.key)
    }

    private val listingPathParts by lazy {
        pathParts.take(pathParts.indexOfLast { it is PathPart.DirectorySeparator<*, *> })
    }
    private val listingVariablePathPartsCount by lazy {
        listingPathParts.count { it is PathPart.VariableExtractor<*> }
    }

    fun findListingByRecord(record: R): Path {
        val mappedRelativePath = listingPathParts.joinToString(separator = "") { pathPart ->
            pathPart.pathPartFromKey(record.key).toString()
        }
        return Paths.get(mappedRelativePath)
    }

    private val recordCandidatePath: Pattern by lazy {
        val joined = pathParts.joinToString("") { it.regex.pattern() }
        Pattern.compile("^$joined$")
    }

    fun isRecord(candidate: Path): Boolean {
        return recordCandidatePath.matcher(candidate.toString()).matches()
    }

    private val listingStart: Path by lazy {
        val relativeListingStart = pathParts.takeWhile { it is PathPart.StaticExtractor<*> }
                .joinToString("")
        root.resolve(relativeListingStart)
    }

    private val listingMaxDepth: Int by lazy {
        val indexOfListingStartPathPart = pathParts.indexOfFirst { it !is PathPart.StaticExtractor<*> }
        val depthOfListingStart = pathParts.take(indexOfListingStartPathPart).count { it is PathPart.DirectorySeparator }
        val trueDepth = pathParts.count { it is PathPart.DirectorySeparator }
        trueDepth - depthOfListingStart
    }

    open fun streamAll(): Stream<Path> {
        return Files.find(
                listingStart,
                listingMaxDepth,
                BiPredicate { candidate: Path, attrs: BasicFileAttributes ->
                    attrs.isRegularFile && isRecord(root.relativize(candidate))
                }
        )
    }

    fun findVariableStringParts(relativeRecordPath: Path): Array<String> {

    }

}