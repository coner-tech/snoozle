package tech.coner.snoozle.db

import assertk.Assert
import assertk.assertions.prop

fun Assert<AbsolutePath>.value() = prop(AbsolutePath::value)