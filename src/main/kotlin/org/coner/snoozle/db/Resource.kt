package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.streams.toList

class Resource<E : Entity>(
        val root: File,
        val kclass: KClass<E>,
        val path: Pathfinder<E>,
        val reader: ObjectReader,
        val writer: ObjectWriter
) {

    constructor(root: File, kclass: KClass<E>, objectMapper: ObjectMapper) : this(
            root,
            kclass,
            Pathfinder(kclass),
            objectMapper.readerFor(kclass.java),
            objectMapper.writerFor(kclass.javaObjectType)
    )

    fun get(vararg ids: Pair<KProperty1<E, UUID>, UUID>): E {
        val file = File(root, path.findEntity(*ids))
        return reader.readValue(file)
    }

    fun put(entity: E) {
        val entityParentPath = path.findParentOfEntity(entity)
        val parent = File(root, entityParentPath)
        if (!parent.exists()) {
            if (!parent.mkdir()) {
                throw FileNotFoundException("""
                    Failed to create parent folder at $entityParentPath.

                    Does its parent exist?
                """.trimIndent())
            }
        }
        val file = File(root, path.findEntity(entity))
        writer.writeValue(file, entity)
    }

    fun delete(entity: E) {
        val file = File(root, path.findEntity(entity))
        file.delete()
    }

    fun list(vararg ids: Pair<KProperty1<E, UUID>, UUID>): List<E> {
        val listingPath = path.findListing(*ids)
        val listing = File(root, listingPath)
        if (!listing.exists()) {
            if (!listing.mkdir())
                throw FileNotFoundException("""
                    Failed to create listing at $listingPath.

                    Does its parent exist?
                """.trimMargin())
        }
        return listing.listFiles()
                .filter { it.isFile && it.extension == "json" }
                .parallelStream()
                .sorted(compareBy(File::getName))
                .map { reader.readValue<E>(it) }
                .toList()
    }
}