package org.coner.snoozle.db.entity

sealed class VersionArgument {

    interface Readable
    interface Writable

    abstract val value: String

    class Specific(version: Int) : VersionArgument(), Readable, Writable {
        override val value = version.toString()
    }
    object Highest : VersionArgument(), Readable {
        override val value = "VersionArgument.Highest"
    }
    object New : VersionArgument(), Writable {
        override val value = "VersionArgument.New"
    }
}