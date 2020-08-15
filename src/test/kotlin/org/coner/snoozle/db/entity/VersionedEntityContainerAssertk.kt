package org.coner.snoozle.db.entity

import assertk.Assert
import assertk.assertions.prop

fun <VE : VersionedEntity> Assert<VersionedEntityContainer<VE>>.entity() = prop("entity") { it.entity }

fun <VE : VersionedEntity> Assert<VersionedEntityContainer<VE>>.version() = prop("version") { it.version }

fun <VE : VersionedEntity> Assert<VersionedEntityContainer<VE>>.ts() = prop("ts") { it.ts }