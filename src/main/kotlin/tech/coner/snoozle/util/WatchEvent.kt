package tech.coner.snoozle.util

import java.nio.file.WatchEvent

fun WatchEvent<*>.prettyToString() =
    "WatchEvent<${context()::class.simpleName}>(context=${context()}, kind=${kind()}, count=${count()})"