package tech.coner.snoozle.db.watch

import assertk.Assert
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop

inline fun <reified R : Any> Assert<Event>.isCreatedTypedInstance() = isInstanceOf(Event.Created::class)
    .transform {
        assertThat(it).recordAsAny().isNotNull().isInstanceOf(R::class)
        it as Event.Created<R>
    }
inline fun <reified R : Any> Assert<Event>.isModifiedTypedInstance() = isInstanceOf(Event.Modified::class)
    .transform {
        assertThat(it).recordAsAny().isNotNull().isInstanceOf(R::class)
        it as Event.Modified<R>
    }
inline fun <reified R : Any> Assert<Event>.isDeletedTypedInstance() = isInstanceOf(Event.Deleted::class)
    .transform {
        assertThat(it).recordAsAny().isNotNull().isInstanceOf(R::class)
        it as Event.Deleted<R>
    }

fun Assert<Event>.isOverflowInstance() = isInstanceOf(Event.Overflow::class)
fun Assert<Event.Record<*>>.recordAsAny() = prop(Event.Record<*>::record)
fun <R> Assert<Event.Record<R>>.record() = prop(Event.Record<R>::record)
fun <R> Assert<Event.Record<R>>.origin() = prop(Event.Record<R>::origin)
