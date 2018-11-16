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

    val entityPathFormat: String = kclass.findAnnotation<EntityPath>()!!.value
    val entityPathReplacementProperties: List<KProperty1<E, UUID>>
    val listingPathFormat: String = entityPathFormat.substring(0..entityPathFormat.lastIndexOf('/'))
    val listingPathReplacementProperties: List<KProperty1<E, UUID>>

    init {
        fun buildPathReplacementProperties(pathReplacementFormat: String): List<KProperty1<E, UUID>> {
            val openBrackets = pathReplacementFormat.count { it == '{' }
            val closeBrackets = pathReplacementFormat.count { it == '}' }
            // TODO: handling for malformed entity path value
            val entityPathReplacements = openBrackets
            val entityPathReplacementProperties = mutableListOf<KProperty1<E, UUID>>()
            var openBracketPosition: Int
            var closeBracketPosition = -1
            for (i in 1 .. entityPathReplacements) {
                openBracketPosition = pathReplacementFormat.indexOf('{', closeBracketPosition + 1)
                closeBracketPosition = pathReplacementFormat.indexOf('}', openBracketPosition)
                val entityPathReplacementPropertyName = pathReplacementFormat.substring(
                        openBracketPosition + 1,
                        closeBracketPosition
                )
                entityPathReplacementProperties.add(
                        kclass.declaredMemberProperties
                                .first { it.name ==  entityPathReplacementPropertyName} as KProperty1<E, UUID>
                )
            }
            return entityPathReplacementProperties.toList()
        }

        this.entityPathReplacementProperties = buildPathReplacementProperties(entityPathFormat)
        this.listingPathReplacementProperties = buildPathReplacementProperties(listingPathFormat)
    }

    fun get(vararg ids: Pair<KProperty1<E, UUID>, UUID>): E {
        val file = file(*ids)
        return objectMapper.readValue(file, kclass.java)
    }

    fun put(entity: E) {
        val file = file(entity)
        file.outputStream().use {
            objectMapper.writeValue(it, entity)
        }
    }

    fun delete(entity: E) {
        val file = file(entity)
        file.delete()
    }

    fun list(vararg ids: Pair<KProperty1<E, UUID>, UUID>): List<E> {
        val files = File(root, findPathToListing(*ids)).listFiles().filter { it.isFile && it.extension == "json" }
        return files.parallelStream()
                .map { file ->
                    file.inputStream().use {
                        objectMapper.readValue(it, kclass.java)
                    }
                }
                .toList()
    }

    private fun findPathToEntity(vararg ids: Pair<KProperty1<E, UUID>, UUID>): String {
        var path = entityPathFormat
        for (id in ids) {
            path = path.replace("{${id.first.name}}", id.second.toString())
        }
        return "$path.json"
    }

    private fun findPathToEntity(entity: E): String {
        var path = entityPathFormat
        for (property in entityPathReplacementProperties) {
            path = path.replace("{${property.name}}", property.get(entity).toString())
        }
        return "$path.json"
    }

    private fun findPathToListing(vararg ids: Pair<KProperty1<E, UUID>, UUID>): String {
        var path = listingPathFormat
        if (ids.isNotEmpty()) {
            for (id in ids) {
                path = path.replace("{${id.first.name}}", id.second.toString())
            }
        }
        return path
    }

    private fun file(vararg ids: Pair<KProperty1<E, UUID>, UUID>): File {
        return File(root, findPathToEntity(*ids))
    }

    private fun file(entity: E): File {
        // TODO: extract to specialized class
        return File(root, findPathToEntity(entity))
    }
}