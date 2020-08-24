package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.PathPart
import org.coner.snoozle.db.Pathfinder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZonedDateTime
import java.util.function.BiPredicate
import java.util.regex.Pattern
import java.util.stream.Stream

class VersionedEntityPathfinder<VE : VersionedEntity<EK>, EK : Key>(
        root: Path,
        pathParts: List<PathPart<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>, *>>
) : Pathfinder<VersionedEntityContainer<VE, EK>, VersionedEntityContainerKey<EK>>(
        root = root,
        pathParts = pathParts
) {

    fun findVersionsListingForInstance(versionedEntity: VE): Path {
        val container = VersionedEntityContainer(
                entity = versionedEntity,
                version = Int.MIN_VALUE,
                ts = ZonedDateTime.now()
        )
        val key = container.key.entity
        val mappedRelativePath = versionedEntityContainerListingPathParts.joinToString(separator = "") {
            it.pathPartFromKey(key)?.toString() ?: ""
        }
        return Paths.get(mappedRelativePath)
    }

    fun findVersionsListingForKey(key: EK): Path {
        return Paths.get(mappedRelativePath)
    }

    private val versionedEntityContainerListingPathParts by lazy {
        pathParts.takeWhile { it !is PathPart.VersionArgumentVariable<*, *, *, *> }
    }

    private val versionedEntityContainerListingCandidatePath: Pattern by lazy {
        val indexOfLastDirectorySeparator = pathParts.indexOfLast { it is PathPart.DirectorySeparator }
        val joined = pathParts.take(indexOfLastDirectorySeparator).joinToString("") { it.regex.pattern() }
        Pattern.compile("^$joined$")
    }

    fun isVersionedEntityContainerListing(candidate: Path): Boolean {
        return versionedEntityContainerListingCandidatePath.matcher(candidate.toString()).matches()
    }

    private val streamAllStart: Path by lazy {
        val relativeListingStart = pathParts.takeWhile { it is PathPart.StaticExtractor<*> }
                .joinToString("")
        root.resolve(relativeListingStart)
    }

    private val streamAllMaxDepth: Int by lazy {
        val indexOfListingStartPathPart = pathParts.indexOfFirst { it !is PathPart.StaticExtractor<*> }
        val depthOfListingStart = pathParts.take(indexOfListingStartPathPart).count { it is PathPart.DirectorySeparator }
        val trueDepth = pathParts.count { it is PathPart.DirectorySeparator } - 1
        trueDepth - depthOfListingStart
    }

    override fun streamAll(): Stream<Path> {
        return Files.find(
                streamAllStart,
                streamAllMaxDepth,
                BiPredicate { candidate: Path, attrs: BasicFileAttributes ->
                    attrs.isDirectory && isVersionedEntityContainerListing(root.relativize(candidate))
                }
        )
    }
}