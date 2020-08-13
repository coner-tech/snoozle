package org.coner.snoozle.db.entity

sealed class VersionArgument {

    abstract val value: String

    class Manual(version: Int) : VersionArgument() {
        override val value = version.toString()
    }
    object Auto : VersionArgument() {
        override val value = "VersionArgument.Auto"
    }
}