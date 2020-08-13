package org.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import org.coner.snoozle.util.readText
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.streams.toList

class VersionedEntityResource<VE : VersionedEntity>(
        private val root: Path,
        internal val versionedEntityDefinition: VersionedEntityDefinition<VE>,
        private val objectMapper: ObjectMapper,
        private val reader: ObjectReader,
        private val writer: ObjectWriter,
        private val path: VersionedEntityPathfinder<VE>
) {

    fun getEntity(vararg args: Any): VersionedEntityContainer<VE> {
        require(args.isNotEmpty()) { "Minimum one argument" }
        val versionArgument = args.singleOrNull { it is VersionArgument}
        val useArgs = args.toMutableList()
        if (versionArgument == null || versionArgument == VersionArgument.Auto) {
            val highestVersion = VersionArgument.Manual(
                    resolveHighestVersion(*useArgs.toTypedArray())
                            ?: throw EntityIoException.NotFound(useArgs)
            )
            when (versionArgument) {
                VersionArgument.Auto -> useArgs[useArgs.lastIndex] = highestVersion
                null -> useArgs += highestVersion
            }
        }
        val entityPath = path.findRecordByArgs(*useArgs.toTypedArray())
        val file = root.resolve(entityPath)
        return read(file)
    }

    fun getAllVersionsOfEntity(vararg args: Any): List<VersionedEntityContainer<VE>> {
        require(args.isNotEmpty()) { "Minimum one argument" }
        val relativeVersionsPath = path.findVersionsListingForArgs(*args)
        val versionsPath = root.resolve(relativeVersionsPath)
        if (!Files.exists(versionsPath)) {
            throw EntityIoException.NotFound("No versions found for entity: $relativeVersionsPath")
        }
        return Files.list(versionsPath)
                .filter { Files.isRegularFile(it) && path.isRecord(root.relativize(it)) }
                .map { read(it) }
                .toList()
                .sorted()
    }

    private fun read(file: Path): VersionedEntityContainer<VE> {
        return if (Files.exists(file)) {
            Files.newInputStream(file).use { inputStream ->
                try {
                    reader.readValue<VersionedEntityContainer<VE>>(inputStream)
                } catch (t: Throwable) {
                    throw EntityIoException.ReadFailure("Failed to read versioned entity: ${file.relativize(root)}", t)
                }
            }
        } else {
            throw EntityIoException.NotFound("Versioned entity not found: ${root.relativize(file)}")
        }
    }

    private fun resolveHighestVersion(vararg args: Any): Int? {
        val relativeVersionsPath = path.findVersionsListingForArgs(*args)
        return readHighestVersion(relativeVersionsPath)
    }

    private fun resolveHighestVersion(entity: VE): Int? {
        val relativeVersionsPath = path.findVersionsListingForInstance(entity)
        return readHighestVersion(relativeVersionsPath)
    }

    private fun readHighestVersion(relativeVersionsPath: Path): Int? {
        val highestVersionMetadata = root.resolve(relativeVersionsPath).resolve("highest.version")
        if (!Files.exists(highestVersionMetadata)) {
            return null
        }
        return try {
            highestVersionMetadata.readText().toInt()
        } catch (t: Throwable) {
            null
        }
    }

    fun listAll(): Stream<VersionedEntityContainer<VE>> {
        return path.listAll()
                .map { versionListing: Path -> {
                    val args = path.extractArgsWithoutVersion(versionListing)
                    getEntity(*args)
                } }
                .map { it() }
    }

    fun put(entity: VE, versionArgument: VersionArgument): VersionedEntityContainer<VE> {
        TODO()
    }

    fun delete(entity: VE, versionArgument: VersionArgument.Manual) {
        TODO()
    }
}