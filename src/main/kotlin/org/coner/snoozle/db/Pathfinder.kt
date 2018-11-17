package org.coner.snoozle.db

import java.lang.Exception
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

class Pathfinder<E : Entity>(
        val kclass: KClass<E>
) {
    private val entityPathFormat: String
    private val entityPathReplaceProperties: List<KProperty1<E, UUID>>
    private val listingPathFormat: String
    private val listingPathReplaceProperties: List<KProperty1<E, UUID>>

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

        val entityPath: EntityPath = kclass.findAnnotation()
            ?: throw EntityDefinitionException("""
                ${kclass.qualifiedName} is not annotated with ${EntityPath::class.qualifiedName}
            """.trimIndent())
        entityPathFormat = entityPath.value
        entityPathReplaceProperties = buildPathReplacementProperties(entityPathFormat)
        listingPathFormat = entityPathFormat.substring(0..entityPathFormat.lastIndexOf('/'))
        listingPathReplaceProperties = buildPathReplacementProperties(listingPathFormat)
    }

    fun findEntity(vararg ids: Pair<KProperty1<E, UUID>, UUID>): String {
        var path = entityPathFormat
        for (id in ids) {
            path = path.replace("{${id.first.name}}", id.second.toString())
        }
        return "$path.json"
    }

    fun findEntity(entity: E): String {
        var path = entityPathFormat
        for (property in entityPathReplaceProperties) {
            path = path.replace("{${property.name}}", property.get(entity).toString())
        }
        return "$path.json"
    }

    fun findListing(vararg ids: Pair<KProperty1<E, UUID>, UUID>): String {
        var path = listingPathFormat
        if (ids.isNotEmpty()) {
            for (id in ids) {
                path = path.replace("{${id.first.name}}", id.second.toString())
            }
        }
        return path
    }
}