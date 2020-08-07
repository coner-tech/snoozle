package org.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import org.coner.snoozle.db.path.Pathfinder
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
        private val path: Pathfinder<VE>
) {

    fun get(vararg args: Any, versionArgument: VersionArgument.Readable): VersionedEntityContainer<VE> {
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
        path.findRecordByArgs(args)
    }

    fun put(entity: VE, versionArgument: VersionArgument.Writable): VersionedEntityContainer<VE> {
        TODO()
    }

    fun delete(entity: VE, versionArgument: VersionArgument.Specific) {
        TODO()
    }
}