package tech.coner.snoozle.db.watch

import tech.coner.snoozle.db.Key
import java.util.regex.Pattern

class Watch<K : Key>(
    val id: Int,
    val directoryPattern: Pattern,
    val filePattern: Pattern
)