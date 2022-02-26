package tech.coner.snoozle.util

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

fun Assert<Path>.doesNotExist(vararg options: LinkOption) = given { actual ->
    if (!Files.notExists(actual, *options)) {
        expected("${show(actual)} to not exist, but it does exist")
    }
}