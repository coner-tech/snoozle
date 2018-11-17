package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.streams.toList

class Resource<E : Entity>(
        val root: File,
        val kclass: KClass<E>,
        val objectMapper: ObjectMapper
) {

    val path: Pathfinder<E>

    init {
        path = Pathfinder(kclass)
    }

    fun get(vararg ids: Pair<KProperty1<E, UUID>, UUID>): E {
        val file = File(root, path.findEntity(*ids))
        return objectMapper.readValue(file, kclass.java)
    }

    fun put(entity: E) {
        val file = File(root, path.findEntity(entity))
        file.outputStream().use {
            objectMapper.writeValue(it, entity)
        }
    }

    fun delete(entity: E) {
        val file = File(root, path.findEntity(entity))
        file.delete()
    }

    fun list(vararg ids: Pair<KProperty1<E, UUID>, UUID>): List<E> {
        return File(root, path.findListing(*ids))
                .listFiles()
                .filter { it.isFile && it.extension == "json" }
                .parallelStream()
                .map { file ->
                    file.inputStream().use {
                        objectMapper.readValue(it, kclass.java)
                    }
                }
                .toList()
    }
}