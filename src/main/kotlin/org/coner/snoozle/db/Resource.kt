package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.streams.toList

class Resource<E : Entity>(
        val root: File,
        val kclass: KClass<E>,
        val objectMapper: ObjectMapper
) {

    val entityPath: EntityPath = kclass.findAnnotation()!!
    val idMemberProperty: KProperty1<E, UUID>
    val listingPath: File

    init {
        val lastOpenBracket = entityPath.format.lastIndexOf('{')
        val lastCloseBracket = entityPath.format.lastIndexOf('}')
        val idMemberPropertyName = entityPath.format.substring(lastOpenBracket + 1, lastCloseBracket)
        idMemberProperty = kclass.declaredMemberProperties
                .first { it.name == idMemberPropertyName } as KProperty1<E, UUID>
        val lastForwardSlash = entityPath.format.lastIndexOf('/')
        listingPath = File(root, entityPath.format.substring(0..lastForwardSlash))
    }

    fun get(id: UUID): E {
        val file = file(id)
        return objectMapper.readValue(file, kclass.java)
    }

    fun put(entity: E) {
        val file = file(idMemberProperty.get(entity))
        file.outputStream().use {
            objectMapper.writeValue(it, entity)
        }
    }

    fun delete(entity: E) {
        val file = file(idMemberProperty.get(entity))
        file.delete()
    }

    fun list(): List<E> {
        val files = listingPath.listFiles()
                .filter { it.isFile && it.extension == "json" }
        return files.parallelStream()
                .map { file ->
                    file.inputStream().use {
                        objectMapper.readValue(it, kclass.java)
                    }
                }
                .toList()
    }

    private fun file(id: UUID): File {
        // TODO: extract to specialized class
        return File(root, entityPath.format.replace("{${idMemberProperty.name}}", id.toString()) + ".json")
    }
}