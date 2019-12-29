package org.coner.snoozle.util

import java.util.*
import java.util.regex.Pattern

val hasUuidPattern by lazy { Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
) }

val isUuidPattern by lazy { Pattern.compile(
        "^${hasUuidPattern.pattern()}$"
) }

fun CharSequence.isValidUuid(): Boolean {
    return isUuidPattern.matcher(this).matches()
}

fun uuid(string: String) = UUID.fromString(string)