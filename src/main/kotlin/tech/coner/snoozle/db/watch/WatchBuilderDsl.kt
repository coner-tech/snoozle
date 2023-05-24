package tech.coner.snoozle.db.watch

import java.util.*
import java.util.regex.Pattern

interface WatchBuilderDsl {
    fun uuidIsAny() : VariableExtractorNode
    fun uuidIsEqualTo(uuid: UUID) : VariableExtractorNode
    fun uuidIsOneOf(uuids: Collection<UUID>) : VariableExtractorNode

    interface VariableExtractorNode {
        val pattern: Pattern
    }
}