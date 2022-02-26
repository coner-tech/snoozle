package tech.coner.snoozle.util

import java.io.File
import java.net.URI
import java.net.URL
import kotlin.io.path.toPath

fun <T> Class<T>.requireResource(name: String): URL = checkNotNull(getResource(name)) {
    "Resource with name not found: $name"
}

fun <T> Class<T>.requireResourceAsFile(name: String): File = requireResource(name)
    .let { it.toURI().toPath().toFile() }

