package tech.coner.snoozle.db.path

import assertk.Assert
import assertk.assertions.prop
import tech.coner.snoozle.db.path.AbsolutePath

fun Assert<AbsolutePath>.value() = prop(AbsolutePath::value)