package org.coner.snoozle

import com.fasterxml.jackson.databind.ObjectMapper
import org.coner.snoozle.annotations.Entity
import org.coner.snoozle.annotations.Id
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

class Resource<E : Any>(
        val root: File,
        val kclass: KClass<E>,
        val objectMapper: ObjectMapper
) {

    val entity: Entity = kclass.findAnnotation()!!
    val idMemberProperty: KProperty1<*, *> = kclass.declaredMemberProperties
            .firstOrNull { it.findAnnotation<Id>() != null }
            ?: throw IllegalArgumentException()

    init {
    }

    inline fun <reified E> get(id: String): E {
        val path = entity.path.replace("{id}", id)
        val file = File(root, "$path.json")
        return objectMapper.readValue(file, E::class.java)
    }
}