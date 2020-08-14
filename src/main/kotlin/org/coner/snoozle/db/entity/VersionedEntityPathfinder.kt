package org.coner.snoozle.db.entity

import org.coner.snoozle.db.path.PathPart
import org.coner.snoozle.db.path.Pathfinder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZonedDateTime
import java.util.function.BiPredicate
import java.util.regex.Pattern
import java.util.stream.Stream

class VersionedEntityPathfinder<VE : VersionedEntity>(
        root: Path,
        pathParts: List<PathPart<VersionedEntityContainer<VE>>>
) : Pathfinder<VersionedEntityContainer<VE>>(
        root = root,
        pathParts = pathParts
) {

    fun findVersionsListingForInstance(versionedEntity: VE): Path {
        val container = VersionedEntityContainer(
                entity = versionedEntity,
                version = Int.MIN_VALUE,
                ts = ZonedDateTime.now()
        )
        val mappedRelativePath = versionedEntityContainerListingPathParts.joinToString(separator = "") { it.forRecord(container) }
        return Paths.get(mappedRelativePath)
    }

    fun findVersionsListingForArgs(vararg args: Any): Path {
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
        // TODO: reduce this to use only one Pattern.matcher(CharSequence) instance
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

    override fun listAll(): Stream<Path> {
        return Files.find(listingStart, listingMaxDepth, BiPredicate { candidate: Path, attrs: BasicFileAttributes ->
            attrs.isDirectory && isVersionedEntityContainerListing(root.relativize(candidate))
        } ).sorted()
    }
}