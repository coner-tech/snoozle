package tech.coner.snoozle.db

import assertk.Assert
import assertk.assertions.prop
import tech.coner.snoozle.db.path.RelativePath

fun Assert<RelativePath>.value() = prop(RelativePath::value)