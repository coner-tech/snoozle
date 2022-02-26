package tech.coner.snoozle.util

import java.nio.file.Path

internal val Path.extension: String
    get() = this.fileName.toString().substringAfterLast('.', "")

internal val Path.nameWithoutExtension: String
    get() = this.fileName.toString().substringBeforeLast(".")

internal fun Path.resolve(vararg paths: String) = paths.fold(this) { acc, subject -> acc.resolve(subject) }