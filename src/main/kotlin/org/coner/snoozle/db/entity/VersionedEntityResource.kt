package org.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import org.coner.snoozle.db.path.Pathfinder
import org.coner.snoozle.util.nameWithoutExtension
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class VersionedEntityResource<VE : VersionedEntity>(
        private val root: Path,
        internal val versionedEntityDefinition: VersionedEntityDefinition<VE>,
        private val objectMapper: ObjectMapper,
        private val reader: ObjectReader,
        private val writer: ObjectWriter,
        private val path: Pathfinder<VersionedEntityContainer<VE>>
) {

    fun getSingleVersionOfEntity(vararg args: Any, versionArgument: VersionArgument.Readable): VersionedEntityContainer<VE> {
        val version: VersionArgument.Specific = when (versionArgument) {
            is VersionArgument.Specific -> versionArgument
            is VersionArgument.Highest -> resolveHighestVersion(*args)
            else -> throw IllegalArgumentException("versionArgument type not handled: ${versionArgument::class.simpleName}")
        }
        val useArgs = args.toMutableList()
                .apply { add(version) }
                .toTypedArray()
        val entityPath = path.findRecordByArgs(useArgs)
        val file = root.resolve(entityPath)
        return read(file)
    }

    fun getAllVersionsOfEntity(vararg args: Any): List<VersionedEntityContainer<VE>> {
        TODO()
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

    private fun resolveHighestVersion(vararg args: Any): VersionArgument.Specific {
        val relativeVersionsPath = path.findPartialBySubsetArgs(args)
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