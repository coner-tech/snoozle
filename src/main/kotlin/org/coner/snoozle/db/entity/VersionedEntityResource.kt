package org.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import org.coner.snoozle.db.path.Pathfinder
import org.coner.snoozle.util.nameWithoutExtension
import java.nio.file.Files
import java.nio.file.Path

class VersionedEntityResource<VE : VersionedEntity, VC : VersionedEntityContainer<VE>>(
        private val root: Path,
        internal val versionedEntityDefinition: VersionedEntityDefinition<VE, VC>,
        private val objectMapper: ObjectMapper,
        private val reader: ObjectReader,
        private val writer: ObjectWriter,
        private val path: Pathfinder<VC>
) {

    fun getEntity(vararg args: Any): VersionedEntityContainer<VE> {
        require(args.isNotEmpty()) { "Minimum one argument" }
        val versionArgument = args.singleOrNull { it is VersionArgument.Readable }
        val useArgs = args.toMutableList()
        when (versionArgument) {
            VersionArgument.Highest -> useArgs[useArgs.lastIndex] = resolveHighestVersion(*useArgs.toTypedArray())
            null -> useArgs.add(resolveHighestVersion(*useArgs.toTypedArray()))
        }
        val entityPath = path.findRecordByArgs(*useArgs.toTypedArray())
        val file = root.resolve(entityPath)
        return read(file)
    }

    fun getAllVersionsOfEntity(vararg args: Any): List<VC> {
        TODO()
    }

    private fun read(file: Path): VC {
        return if (Files.exists(file)) {
            Files.newInputStream(file).use { inputStream ->
                try {
                    reader.readValue<VC>(inputStream)
                } catch (t: Throwable) {
                    throw EntityIoException.ReadFailure("Failed to read versioned entity: ${file.relativize(root)}", t)
                }
            }
        } else {
            throw EntityIoException.NotFound("Versioned entity not found: ${root.relativize(file)}")
        }
    }

    private fun resolveHighestVersion(vararg args: Any): VersionArgument.Specific {
        val relativeVersionsPath = path.findVersions(*args)
        val versionsPath = root.resolve(relativeVersionsPath)
        if (!Files.exists(versionsPath)) {
            throw EntityIoException.NotFound("No versions found for entity: $relativeVersionsPath")
        }
        val version = Files.list(versionsPath)
                .filter { Files.isRegularFile(it) && path.isRecord(root.relativize(it)) }
                .map { it.nameWithoutExtension.toInt() }
                .sorted()
                .max(compareBy { it })
                .orElseThrow { throw EntityIoException.NotFound("No versions found for entity: $relativeVersionsPath") }
        return VersionArgument.Specific(version)
    }

    fun put(entity: VE, versionArgument: VersionArgument.Writable): VersionedEntityContainer<VE> {
        TODO()
    }

    fun delete(entity: VE, versionArgument: VersionArgument.Specific) {
        TODO()
    }
}