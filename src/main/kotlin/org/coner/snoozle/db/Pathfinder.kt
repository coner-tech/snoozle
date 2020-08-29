package org.coner.snoozle.db

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import java.util.regex.Pattern
import java.util.stream.Stream

open class Pathfinder<K : Key, R : Record<K>>(
        protected val root: Path,
        protected val pathParts: List<PathPart<K, R, *>>
) {

    fun findRecord(key: K): Path {
        val relativePath = pathParts.joinToString(separator = "") { pathPart ->
            when (pathPart) {
                is PathPart.StaticExtractor<*> -> pathPart.value
                is PathPart.VariableExtractor<*, *> -> (pathPart as PathPart.VariableExtractor<K, *>).pathPartFromKey(key).toString()
                else -> throw IllegalStateException("Unhandled pathPart type: $pathPart")
            }
        }
        return Paths.get(relativePath)
    }

    private val recordCandidatePath: Pattern by lazy {
        val joined = pathParts.joinToString("") { it.regex.pattern() }
        Pattern.compile("^$joined$")
    }

    fun isRecord(candidate: Path): Boolean {
        return recordCandidatePath.matcher(candidate.toString()).matches()
    }

    private val listingStart: Path by lazy {
        val relativeListingStart = pathParts
                .takeWhile { it is PathPart.StaticExtractor<*> }
                .joinToString("") { (it as PathPart.StaticExtractor<*>).value }
        root.resolve(relativeListingStart)
    }

    private val listingMaxDepth: Int by lazy {
        val indexOfListingStartPathPart = pathParts.indexOfFirst { it !is PathPart.StaticExtractor<*> }
        val depthOfListingStart = pathParts.take(indexOfListingStartPathPart).count { it is PathPart.DirectorySeparator }
        val trueDepth = pathParts.count { it is PathPart.DirectorySeparator }
        1 + (trueDepth - depthOfListingStart)
    }

    fun streamAll(): Stream<Path> {
        val start = listingStart
        val maxDepth = listingMaxDepth
        return Files.find(
                start,
                maxDepth,
                BiPredicate { candidate: Path, attrs: BasicFileAttributes ->
                    attrs.isRegularFile && isRecord(root.relativize(candidate))
                }
        )
                .map { root.relativize(it) }
    }

    fun findVariableStringParts(relativeRecordPath: Path): Array<String> {
        var remainingPathParts = relativeRecordPath.toString()
        val extractedVariables = mutableListOf<Any>()
        pathParts.forEach { pathPart ->
            val matcher = pathPart.regex.matcher(remainingPathParts)
            check(matcher.find() && matcher.start() == 0) { "Not a relative record path" }
            if (pathPart is PathPart.VariableExtractor<*, *>) {
                extractedVariables += matcher.group()
            }
            remainingPathParts = remainingPathParts.substring(matcher.end())
        }
        check(remainingPathParts.isEmpty()) { "Remaining path parts must be empty but were not"}
        return extractedVariables.map { it.toString() }.toTypedArray()
    }

}