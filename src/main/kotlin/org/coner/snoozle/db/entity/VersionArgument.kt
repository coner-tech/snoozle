package org.coner.snoozle.db.entity

sealed class VersionArgument {

    abstract val value: String

    class Manual(val version: Int) : VersionArgument() {
        override val value = version.toString()
    }
    object Auto : VersionArgument() {
        override val value = "VersionArgument.Auto"
    }
}