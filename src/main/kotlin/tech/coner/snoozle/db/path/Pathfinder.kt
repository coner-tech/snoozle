package tech.coner.snoozle.db.path

import tech.coner.snoozle.db.Key
import tech.coner.snoozle.db.Record
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.regex.Pattern
import java.util.stream.Stream

open class Pathfinder<K : Key, R : Record<K>>(
    protected val root: AbsolutePath,
    protected val pathParts: List<PathPart<K, R, *>>
) {

    fun findRecord(key: K): RelativePath {
        val relativePath = pathParts.joinToString("") { pathPart ->
            when (pathPart) {
                is PathPart.StaticExtractor<*> -> pathPart.value
                is PathPart.VariableExtractor<*, *> -> (pathPart as PathPart.VariableExtractor<K, *>).pathPartFromKey(key).toString()
                else -> throw IllegalStateException("Unhandled pathPart type: $pathPart")
            }
        }
        return Paths.get(relativePath).asRelative()
    }

    val recordCandidatePath: Pattern by lazy {
        val joined = pathParts.joinToPathPatternString { it.regex.pattern() }
        Pattern.compile("^$joined$")
    }

    val recordParentCandidatePath: Pattern by lazy {
        val indexOfLastDirectorySeparator = pathParts.indexOfLast {
            it is PathPart.DirectorySeparator<K, R>
        }
        pathParts
            .take(indexOfLastDirectorySeparator)
            .joinToPathPatternString { it.regex.pattern() }
            .let { Pattern.compile("^$it$") }
    }

    fun isRecordParent(candidate: RelativePath): Boolean {
        return recordParentCandidatePath.matcher(candidate.value.toString()).matches()
    }

    fun isRecord(candidate: RelativePath): Boolean {
        return recordCandidatePath.matcher(candidate.value.toString()).matches()
    }

    private val listingStart: AbsolutePath by lazy {
        val relativeListingStart = pathParts
                .takeWhile { it is PathPart.StaticExtractor<*> }
                .joinToString("") { (it as PathPart.StaticExtractor<*>).value }
        root.value.resolve(relativeListingStart).asAbsolute()
    }

    private val listingMaxDepth: Int by lazy {
        val indexOfListingStartPathPart = pathParts.indexOfFirst { it !is PathPart.StaticExtractor<*> }
        val depthOfListingStart = pathParts.take(indexOfListingStartPathPart).count { it is PathPart.DirectorySeparator }
        val trueDepth = pathParts.count { it is PathPart.DirectorySeparator }
        1 + (trueDepth - depthOfListingStart)
    }

    fun streamAll(): Stream<RelativePath> {
        val start = listingStart
        val maxDepth = listingMaxDepth
        return try {
            Files.find(
                    start.value,
                    maxDepth,
                    { candidate: Path, attrs: BasicFileAttributes ->
                        attrs.isRegularFile && isRecord(root.value.relativize(candidate).asRelative())
                    }
            )
                    .map { root.value.relativize(it).asRelative() }
        } catch (noSuchFileException: NoSuchFileException) {
            Stream.empty()
        }
    }

    fun findVariableStringParts(relativeRecordPath: RelativePath): Array<String> {
        var remainingPathParts = relativeRecordPath.value.toString()
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

    private fun <K : Key, R : Record<K>> List<PathPart<K, R, *>>.joinToPathPatternString(
        transform: (PathPart<K, R, *>) -> CharSequence
    ): String = joinToString(
        separator = "",
        prefix = "^",
        postfix = "$",
        transform = transform
    )

}