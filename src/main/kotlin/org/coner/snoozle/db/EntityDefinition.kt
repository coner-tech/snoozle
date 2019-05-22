package org.coner.snoozle.db

import kotlin.reflect.KClass

data class EntityDefinition<E : Entity>(
        val kClass: KClass<E>
)