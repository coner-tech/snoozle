package org.coner.snoozle.db.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import org.coner.snoozle.db.Key
import org.coner.snoozle.util.readText
import java.nio.file.*
import java.time.ZonedDateTime
import java.util.function.Predicate
import java.util.stream.Stream
import kotlin.streams.toList

class VersionedEntityResource<VE : VersionedEntity<EK>, EK : Key>(
        private val root: Path,
        internal val definition: VersionedEntityDefinition<VE, EK>,
        private val reader: ObjectReader,
        private val writer: ObjectWriter,
        private val path: VersionedEntityPathfinder<VE, EK>,
        private val key: VersionedEntityKeyParser<VE, EK>
) {

    fun get(
            key: EK,
            version: VersionArgument = VersionArgument.Auto
    ): VersionedEntityContainer<VE, EK> {
        val containerKey = VersionedEntityContainerKey(
                entity = key,
                version = when (version) {
                    VersionArgument.Auto -> resolveHighestVersion(key)
                            ?: throw EntityIoException.NotFound(key)
                    is VersionArgument.Manual -> version.version
                }
        )
        val entityPath = path.findRecord(containerKey)
        val file = root.resolve(entityPath)
        return read(file)
    }

    fun getAllVersions(key: EK): List<VersionedEntityContainer<VE, EK>> {
        val relativeVersionsPath = path.findVersionsListingForKey(key)
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

    private fun read(file: Path): VersionedEntityContainer<VE, EK> {
        return if (Files.exists(file)) {
            Files.newInputStream(file).use { inputStream ->
                try {
                    reader.readValue<VersionedEntityContainer<VE, EK>>(inputStream)
                } catch (t: Throwable) {
                    throw EntityIoException.ReadFailure("Failed to read versioned entity: ${file.relativize(root)}", t)
                }
            }
        } else {
            throw EntityIoException.NotFound("Versioned entity not found: ${root.relativize(file)}")
        }
    }

    private fun resolveHighestVersion(key: EK): Int? {
        val relativeVersionsPath = path.findVersionsListingForKey(key)
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

    fun streamAll(keyFilter: Predicate<EK>? = null): Stream<VersionedEntityContainer<VE, EK>> {
        val allPathsMappedToKeys = path.streamAll()
                .map { recordPath: Path -> key.parse(recordPath) }
        val allKeysForRead = keyFilter?.let { allPathsMappedToKeys.filter(it) } ?: allPathsMappedToKeys
        return allKeysForRead.map { read(path.findRecord()) }
    }

    fun put(entity: VE, versionArgument: VersionArgument): VersionedEntityContainer<VE> {
        val highestVersion = resolveHighestVersion(entity)
        val useVersionArgument = when(versionArgument) {
            is VersionArgument.Manual -> {
                if (highestVersion != null) {
                    require(versionArgument.version == highestVersion + 1) {
                        "Version argument does not correctly increment version"
                    }
                } else {
                    require(versionArgument.version == 0) {
                        "Version argument should be zero"
                    }
                }
                VersionArgument.Manual(versionArgument.version)
            }
            VersionArgument.Auto -> when (highestVersion) {
                null -> VersionArgument.Manual(0)
                else -> VersionArgument.Manual(highestVersion + 1)
            }
        }
        val container = VersionedEntityContainer(
                entity = entity,
                version = useVersionArgument.version,
                ts = ZonedDateTime.now()
        )
        val relativeRecordPath = path.findRecord(container)
        val recordPath = root.resolve(relativeRecordPath)
        val temporaryRecordPath = recordPath.resolveSibling("${container.version}.tmp")
        if (!Files.exists(recordPath.parent)) {
            try {
                Files.createDirectories(recordPath.parent)
            } catch (t: Throwable) {
                throw EntityIoException.WriteFailure("Failed to create version listing for record: $relativeRecordPath", t)
            }
        }
        try {
            Files.newOutputStream(temporaryRecordPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW).use { temporaryRecordOutputStream ->
                writer.writeValue(temporaryRecordOutputStream, container)
            }
        } catch (t: Throwable) {
            throw EntityIoException.WriteFailure("Failed to write record to temporary file")
        }
        fun attemptToDeleteTemporaryRecord() {
            try {
                Files.deleteIfExists(temporaryRecordPath)
            } catch (t: Throwable) {
                // best-effort mess clean-up, don't actually care
            }
        }
        val temporaryHighestVersionMetadata = recordPath.resolveSibling("highest.version.tmp")
        try {
            Files.writeString(temporaryHighestVersionMetadata, useVersionArgument.value, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)
        } catch (t: Throwable) {
            attemptToDeleteTemporaryRecord()
            throw EntityIoException.WriteFailure("Failed to write highest version metadata to temporary file", t)
        }
        fun attemptToDeleteTemporaryHighestVersionMetadata() {
            try {
                Files.deleteIfExists(temporaryHighestVersionMetadata)
            } catch (t: Throwable) {
                // best-effort mess clean-up, don't actually care
            }
        }
        try {
            Files.move(temporaryRecordPath, recordPath, StandardCopyOption.ATOMIC_MOVE)
        } catch (t: Throwable) {
            attemptToDeleteTemporaryRecord()
            attemptToDeleteTemporaryHighestVersionMetadata()
            throw EntityIoException.WriteFailure("Failed to move temporary record into permanent place", t)
        }
        try {
            val highestVersionMetadataMoveOptions = mutableListOf<CopyOption>(StandardCopyOption.ATOMIC_MOVE)
            if (container.version > 0) {
                highestVersionMetadataMoveOptions += StandardCopyOption.REPLACE_EXISTING
            }
            Files.move(
                    temporaryHighestVersionMetadata,
                    recordPath.resolveSibling("highest.version"),
                    *highestVersionMetadataMoveOptions.toTypedArray()
            )
        } catch (t: Throwable) {
            attemptToDeleteTemporaryRecord()
            attemptToDeleteTemporaryHighestVersionMetadata()
            throw EntityIoException.WriteFailure("Failed to move highest version metadata file into permanent place", t)
        }
        return container
    }

    fun delete(entity: VE, versionArgument: VersionArgument.Manual) {
        val highestVersion = resolveHighestVersion(entity)
                ?: throw EntityIoException.NotFound("Highest version not found. Record doesn't exist")
        require(highestVersion == versionArgument.version) {
            "Version argument given doesn't match highest version"
        }
        val relativeVersionsListing = path.findVersionsListingForInstance(entity)
        val versionsListingAsFile = root.resolve(relativeVersionsListing).toFile()
        if (!versionsListingAsFile.deleteRecursively()) {
            throw EntityIoException.WriteFailure("Failed to delete record")
        }
    }
}