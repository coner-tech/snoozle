package org.coner.snoozle.util

import java.util.regex.Pattern

private val pattern by lazy { Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
) }

fun CharSequence.isValidUuid(): Boolean {
    return pattern.matcher(this).matches()
}