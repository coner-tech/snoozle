package tech.coner.snoozle.db.path

import java.nio.file.Path

data class RelativePath(val value: Path) {
    init {
        require(!value.isAbsolute) { "$value is an absolute path" }
    }
}

internal fun Path.asRelative() = RelativePath(this)