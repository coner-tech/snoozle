package org.coner.snoozle.util

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

internal val Path.extension: String
    get() = this.fileName.toString().substringAfterLast('.', "")

internal val Path.nameWithoutExtension: String
    get() = this.fileName.toString().substringBeforeLast(".")

internal fun Path.readText(charset: Charset = Charsets.UTF_8): String {
    return Files.readAllBytes(this).toString(charset)
}