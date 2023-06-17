package tech.coner.snoozle.db.util

import tech.coner.snoozle.db.Database
import java.nio.file.Path
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

fun Path.setWritableRecursively(writable: Boolean) {
    walk(PathWalkOption.INCLUDE_DIRECTORIES)
}