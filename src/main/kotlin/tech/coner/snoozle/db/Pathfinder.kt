package tech.coner.snoozle.db

import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.regex.Pattern
import java.util.stream.Stream

open class Pathfinder<K : tech.coner.snoozle.db.Key, R : Record<K>>(
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

    private val recordParentCandidatePath: Pattern by lazy {
        val indexOfLastDirectorySeparator = pathParts.indexOfLast {
            it is PathPart.DirectorySeparator<K, R>
        }
        pathParts
            .take(indexOfLastDirectorySeparator)
            .joinToString("") { it.regex.pattern() }
            .let { Pattern.compile("^$it$") }
    }

    fun isRecordParent(candidate: Path): Boolean {
        return recordParentCandidatePath.matcher(candidate.toString()).matches()
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
        return try {
            Files.find(
                    start,
                    maxDepth,
                    { candidate: Path, attrs: BasicFileAttributes ->
                        attrs.isRegularFile && isRecord(root.relativize(candidate))
                    }
            )
                    .map { root.relativize(it) }
        } catch (noSuchFileException: NoSuchFileException) {
            Stream.empty()
        }
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