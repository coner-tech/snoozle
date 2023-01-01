package tech.coner.snoozle.db

import assertk.Assert
import assertk.assertions.matchesPredicate
import java.nio.file.Path

fun Assert<Path>.isAbsolute() = apply { matchesPredicate { it.isAbsolute } }
fun Assert<Path>.isRelative() = apply { matchesPredicate { !it.isAbsolute } }