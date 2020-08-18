package org.coner.snoozle.db.path

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.Record
import java.io.IOException
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

    fun findRecord(record: R): Path {
        val mappedRelativePath = pathParts.joinToString(separator = "") { pathPart ->
            pathPart.pathPartFromKey(record.key).toString()
        }
        return Paths.get(mappedRelativePath)
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
        val joined = pathParts.map { it.regex.pattern() }.joinToString("")
        Pattern.compile("^$joined$")
    }

    fun isRecord(candidate: Path): Boolean {
        return recordCandidatePath.matcher(candidate.toString()).matches()
    }


    open fun streamAll(): Stream<Path> {
        return Files.find(listingStart, listingMaxDepth, BiPredicate { candidate: Path, attrs: BasicFileAttributes ->
                    attrs.isDirectory && isRecord(root.relativize(candidate))
        } ).sorted()
    }

}