package tech.coner.snoozle.db.path

import java.nio.file.Path

data class AbsolutePath(val value: Path) {
    init {
        require(value.isAbsolute) { "$value is not an absolute path" }
    }
}

internal fun Path.asAbsolute() = AbsolutePath(this)