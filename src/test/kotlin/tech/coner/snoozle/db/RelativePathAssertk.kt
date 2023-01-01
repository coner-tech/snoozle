package tech.coner.snoozle.db

import assertk.Assert
import assertk.assertions.prop

fun Assert<RelativePath>.value() = prop(RelativePath::value)