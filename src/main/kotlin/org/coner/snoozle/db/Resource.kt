package org.coner.snoozle.db

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

class Resource<E : Entity>(
        val root: File,
        val kclass: KClass<E>,
        val objectMapper: ObjectMapper
) {

    val path: Path = kclass.findAnnotation()!!
    val idMemberProperty: KProperty1<E, UUID>

    init {
        val lastOpenBracket = path.format.lastIndexOf('{')
        val lastCloseBracket = path.format.lastIndexOf('}')
        val idMemberPropertyName = path.format.substring(lastOpenBracket + 1, lastCloseBracket)
        idMemberProperty = kclass.declaredMemberProperties
                .first { it.name == idMemberPropertyName } as KProperty1<E, UUID>
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

    private fun file(id: UUID): File {
        // TODO: extract to specialized class
        return File(root, path.format.replace("{${idMemberProperty.name}}", id.toString()) + ".json")
    }
}