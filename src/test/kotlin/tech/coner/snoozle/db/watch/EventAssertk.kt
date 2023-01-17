package tech.coner.snoozle.db.watch

import assertk.Assert
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop

fun <ID : Any, C : Any> Assert<Event<ID, C>>.origin() = prop(Event<ID, C>::origin)

inline fun <reified ID : Any, reified C : Any> Assert<Event<ID, C>>.isCreatedTypedInstance() =
    isInstanceOf(Event.Created::class)
        .transform {
            assertThat(it).recordIdAsAny().isNotNull().isInstanceOf(ID::class)
            assertThat(it).recordContentAsAny().isNotNull().isInstanceOf(C::class)
            it as Event.Created<ID, C>
        }
inline fun <reified ID : Any, reified C : Any> Assert<Event<ID, C>>.isModifiedTypedInstance() =
    isInstanceOf(Event.Modified::class)
        .transform {
            assertThat(it).recordIdAsAny().isNotNull().isInstanceOf(ID::class)
            assertThat(it).recordContentAsAny().isNotNull().isInstanceOf(C::class)
            it as Event.Modified<ID, C>
        }
inline fun <reified ID : Any, C : Any> Assert<Event<ID, C>>.isDeletedTypedInstance() =
    isInstanceOf(Event.Deleted::class)
        .transform {
            assertThat(it).recordIdAsAny().isNotNull().isInstanceOf(ID::class)
            it as Event.Deleted<ID, C>
        }

fun <ID : Any, C : Any> Assert<Event<ID, C>>.isOverflowInstance() = isInstanceOf(Event.Overflow::class)

fun Assert<Event.Record<*, *>>.recordIdAsAny() = prop(Event.Record<*, *>::recordId)
fun <ID : Any, C : Any> Assert<Event.Record<ID, C>>.recordId() = prop(Event.Record<ID, C>::recordId)

fun Assert<Event.Exists<*, *>>.recordContentAsAny() = prop(Event.Exists<*, *>::recordContent)
fun <ID : Any, C : Any> Assert<Event.Exists<ID, C>>.recordContent() = prop(Event.Exists<ID, C>::recordContent)
